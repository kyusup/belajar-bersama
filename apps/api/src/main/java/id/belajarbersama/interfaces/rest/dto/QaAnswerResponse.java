package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record QaAnswerResponse(
        UUID id,
        UUID questionId,
        UUID authorId,
        String authorDisplayName,
        String body,
        boolean accepted,
        int usefulCount,
        boolean markedUseful,
        Instant createdAt) {}
