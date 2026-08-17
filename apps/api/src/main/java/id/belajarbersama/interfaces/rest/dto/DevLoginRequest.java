package id.belajarbersama.interfaces.rest.dto;

public record DevLoginRequest(
        String provider, String subject, String displayName, String avatarUrl) {}
