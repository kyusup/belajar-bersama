package id.belajarbersama.interfaces.rest.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AnswerRequest(Map<UUID, List<UUID>> answers) {
    public Map<UUID, Set<UUID>> asSets() {
        if (answers == null) {
            return Map.of();
        }
        return answers.entrySet().stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue() == null
                                                ? Set.of()
                                                : Set.copyOf(entry.getValue())));
    }
}
