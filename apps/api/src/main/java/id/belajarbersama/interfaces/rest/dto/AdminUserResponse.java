package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id, String displayName, String status, Instant createdAt, List<String> storedRoles) {}
