package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QaQuestionResponse(
        UUID id,
        String title,
        String body,
        UUID authorId,
        String authorDisplayName,
        UUID subjectId,
        UUID contentId,
        String status,
        UUID acceptedAnswerId,
        Instant createdAt,
        Instant updatedAt,
        List<QaAnswerResponse> answers) {}
