package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record LearningResume(UserId userId, UUID contentId, UUID courseId, Instant updatedAt) {}
