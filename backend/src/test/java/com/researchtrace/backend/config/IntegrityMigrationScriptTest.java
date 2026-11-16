package com.researchtrace.backend.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrityMigrationScriptTest {

    @Test
    void migrationIsAdditiveAndDoesNotDeleteData() throws IOException {
        String sql = java.nio.file.Files.readString(
            java.nio.file.Path.of("..", "docker", "initdb", "02-integrity.sql"),
            StandardCharsets.UTF_8).toLowerCase();
        assertThat(sql).doesNotContain("drop table public");
        assertThat(sql).doesNotContain("delete from public");
        assertThat(sql).doesNotContain("truncate ");
        assertThat(sql).contains("not valid");
        assertThat(sql).contains("uk_users_email_lower");
        assertThat(sql).contains("uk_project_member_project_user");
        assertThat(sql).contains("fk_task_milestone");
        assertThat(sql).contains("no rows were changed");
    }
}
