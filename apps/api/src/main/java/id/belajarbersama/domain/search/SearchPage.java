package id.belajarbersama.domain.search;

import java.util.List;

public record SearchPage(List<SearchHit> items, int page, int size, long totalItems) {}
