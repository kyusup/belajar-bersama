package id.belajarbersama.domain.learning;

public record ProgressSnapshot(int completed, int total, int percent) {
    public static ProgressSnapshot of(int completed, int total) {
        int safeTotal = Math.max(total, 0);
        int safeCompleted = Math.min(Math.max(completed, 0), safeTotal);
        int percent = safeTotal == 0 ? 0 : (safeCompleted * 100) / safeTotal;
        return new ProgressSnapshot(safeCompleted, safeTotal, percent);
    }
}
