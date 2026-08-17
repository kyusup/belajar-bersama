export const PERMISSIONS = [
  "USER_READ_SELF",
  "CONTENT_READ_PUBLISHED",
  "LEARNING_PROGRESS_MANAGE",
  "BOOKMARK_MANAGE",
  "QUIZ_HISTORY_READ",
  "QA_CREATE",
  "QA_ASK",
  "QA_ANSWER",
  "QA_MARK_USEFUL",
  "QA_ACCEPT_ANSWER",
  "CONTENT_REPORT",
  "CONTENT_CREATE",
  "CONTENT_EDIT_OWN",
  "CONTENT_UPDATE_DRAFT",
  "CONTENT_SUBMIT",
  "CONTENT_REVIEW",
  "CONTENT_REQUEST_CHANGES",
  "CONTENT_APPROVE",
  "CONTENT_PUBLISH",
  "CONTENT_ARCHIVE",
  "CONTENT_MODERATE",
  "CONTENT_REPORT_REVIEW",
  "VERIFICATION_APPLY",
  "VERIFICATION_REVIEW",
  "VERIFICATION_APPROVE",
  "VERIFICATION_REVOKE",
  "VERIFICATION_GRANT",
  "TAXONOMY_MANAGE",
  "USER_MANAGE",
  "ROLE_MANAGE",
  "ROLE_ASSIGN",
  "SYSTEM_ADMIN",
  "AUDIT_READ",
] as const;

export type Permission = (typeof PERMISSIONS)[number];

export type ApiError = {
  code: string;
  message: string;
  details: Record<string, unknown>;
  correlationId?: string;
};

export type ComponentStatus = {
  status: "UP" | "DOWN" | string;
  provider?: string | null;
  detail?: string | null;
};

export type PlatformStatus = {
  service: string;
  version: string;
  status: "UP" | "DOWN" | string;
  components: Record<string, ComponentStatus>;
};

export type HealthResponse = {
  status: string;
  service: string;
};
