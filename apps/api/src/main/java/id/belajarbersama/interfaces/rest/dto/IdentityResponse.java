package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record IdentityResponse(UUID id, String provider, String issuer) {}
