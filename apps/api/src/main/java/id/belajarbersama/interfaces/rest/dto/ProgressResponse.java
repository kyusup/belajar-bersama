package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record ProgressResponse(
        UUID contentId, int completed, int total, int percent, boolean lessonCompleted) {}
