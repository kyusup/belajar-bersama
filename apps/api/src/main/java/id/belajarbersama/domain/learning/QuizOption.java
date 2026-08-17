package id.belajarbersama.domain.learning;

import java.util.UUID;

public record QuizOption(
        UUID id, UUID questionId, int sortOrder, String label, String text, boolean correct) {}
