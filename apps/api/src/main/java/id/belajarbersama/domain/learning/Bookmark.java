package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record Bookmark(UserId userId, UUID contentId, Instant createdAt) {}
