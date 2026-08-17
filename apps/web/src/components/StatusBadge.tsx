import { copy } from "@/lib/i18n/id";

const LABELS: Record<string, string> = {
  DRAFT: copy.statusDraft,
  SUBMITTED: copy.statusSubmitted,
  IN_REVIEW: copy.statusInReview,
  CHANGES_REQUESTED: copy.statusChangesRequested,
  APPROVED: copy.statusApproved,
  PUBLISHED: copy.statusPublished,
  ARCHIVED: copy.statusArchived,
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`badge status-${status.toLowerCase()}`}>
      {LABELS[status] ?? status}
    </span>
  );
}
