package id.belajarbersama.interfaces.rest.dto;

import java.util.List;

public record QaPageResponse(List<QaQuestionResponse> items, int page, int size, long totalItems) {}
