package com.researchtrace.backend;

import com.researchtrace.backend.ai.MissingGeminiKeyContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.addInitializers(new MissingGeminiKeyContextInitializer());
        application.run(args);
    }

    /**
     * Loads local environment variables from .env during normal application startup.
     * Looks in the working directory, root directory, backend directory, and parent directories.
     * Never overrides environment variables or system properties already set.
     */
    public static void loadDotEnv() {
        List<File> candidates = new ArrayList<>();
        File userDir = new File(System.getProperty("user.dir", "."));
        candidates.add(new File(userDir, ".env"));
        candidates.add(new File(userDir, "backend/.env"));
        candidates.add(new File(userDir, "../backend/.env"));
        candidates.add(new File(userDir, "../.env"));

        try {
            var codeSource = BackendApplication.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                File codeLocation = new File(codeSource.getLocation().toURI());
                File dir = codeLocation.isDirectory() ? codeLocation : codeLocation.getParentFile();
                if (dir != null) {
                    candidates.add(new File(dir, ".env"));
                    if (dir.getParentFile() != null) {
                        candidates.add(new File(dir.getParentFile(), ".env"));
                        candidates.add(new File(dir.getParentFile(), "backend/.env"));
                        if (dir.getParentFile().getParentFile() != null) {
                            candidates.add(new File(dir.getParentFile().getParentFile(), ".env"));
                            candidates.add(new File(dir.getParentFile().getParentFile(), "backend/.env"));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isFile()) {
                loadFile(candidate);
                break;
            }
        }
    }

    private static void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

}
