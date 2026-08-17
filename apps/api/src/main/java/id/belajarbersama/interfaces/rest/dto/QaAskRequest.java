package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record QaAskRequest(String title, String body, UUID subjectId, UUID contentId) {}
