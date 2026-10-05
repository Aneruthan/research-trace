package com.researchtrace.backend.ai;

import java.util.List;

/**
 * AI boundary for task extraction. Implementations return candidate tasks as plain data
 * and must not read or write the database.
 */
public interface TaskExtractor {

    List<ExtractedTask> extract(TaskExtractionInput input);
}
