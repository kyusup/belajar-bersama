package id.belajarbersama.interfaces.rest.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        String status,
        Set<String> roles,
        Set<String> storedRoles,
        Set<String> permissions,
        List<IdentityResponse> identities,
        Set<UUID> verifiedCompetencyIds) {}
