import { apiGet, apiRequest } from "./client";

export type TaxonomyItem = {
  id: string;
  slug: string;
  name: string;
  description: string | null;
};

export type LicenseItem = {
  code: string;
  name: string;
  description: string;
};

export type ContentBlock = {
  type: string;
  level?: number | null;
  text?: string | null;
  ordered?: boolean | null;
  items?: string[] | null;
  language?: string | null;
  href?: string | null;
};

export type ContentBody = {
  blocks: ContentBlock[];
};

export type Source = {
  id?: string;
  title: string;
  author?: string | null;
  publisher?: string | null;
  url?: string | null;
  publicationInfo?: string | null;
  notes?: string | null;
};

export type ContentReview = {
  id: string;
  submissionId: string;
  revisionId: string;
  reviewerId: string;
  decision: string | null;
  comment: string | null;
  createdAt: string;
  decidedAt: string | null;
};

export type ContentRevision = {
  id: string;
  revisionNumber: number;
  title: string;
  summary: string;
  body: ContentBody;
  license: string;
  changeSummary: string | null;
  createdBy: string;
  createdAt: string;
  competencyIds: string[];
  sources: Source[];
};

export type ContentChild = {
  id: string;
  slug: string;
  kind: string;
  title: string;
  sortOrder: number;
  required: boolean;
  children: ContentChild[];
};

export type ContentDetail = {
  id: string;
  kind: string;
  slug: string;
  makerId: string;
  makerDisplayName: string;
  subjectId: string;
  subjectName: string;
  educationLevelId: string;
  educationLevelName: string;
  parentId: string | null;
  status: string;
  publiclyVisible: boolean;
  currentRevisionId: string;
  publishedRevisionId: string | null;
  currentRevisionNumber: number;
  createdAt: string;
  updatedAt: string;
  currentRevision: ContentRevision;
  reviews: ContentReview[];
  sortOrder?: number;
  required?: boolean;
  children?: ContentChild[];
  quiz?: QuizDraft;
};

export type Submission = {
  id: string;
  contentId: string;
  revisionId: string;
  makerId: string;
  status: string;
  assignedCheckerId: string | null;
  createdAt: string;
  title?: string | null;
};

export type SearchPage = {
  items: { id: string; type: string; title: string; slug: string; summary: string }[];
  page: number;
  size: number;
  totalItems: number;
};

export type QuizOptionDraft = {
  label: string;
  text: string;
  correct: boolean;
};

export type QuizQuestionDraft = {
  type: string;
  prompt: string;
  explanation: string;
  difficulty: string;
  options: QuizOptionDraft[];
};

export type QuizDraft = {
  passingScore?: number | null;
  maxAttempts?: number | null;
  required?: boolean;
  questions: QuizQuestionDraft[];
};

export type ContentDraft = {
  kind?: string;
  title: string;
  summary: string;
  subjectId: string;
  educationLevelId: string;
  parentId?: string | null;
  competencyIds: string[];
  license: string;
  body: ContentBody;
  sources: Source[];
  changeSummary?: string;
  sortOrder?: number;
  required?: boolean;
  quiz?: QuizDraft;
};

export function contentHref(kind: string | undefined, slug: string): string {
  switch (kind) {
    case "COURSE":
      return `/kursus/${slug}`;
    case "LEARNING_PATH":
      return `/jalur/${slug}`;
    case "QUIZ":
      return `/kuis/${slug}`;
    case "QA_QUESTION":
      return `/tanya/${slug}`;
    default:
      return `/materi/${slug}`;
  }
}

export function searchHref(type: string | undefined, slug: string): string {
  return contentHref(type, slug);
}

export function fetchSubjects(): Promise<TaxonomyItem[]> {
  return apiGet<TaxonomyItem[]>("/api/v1/public/subjects");
}

export function fetchEducationLevels(): Promise<TaxonomyItem[]> {
  return apiGet<TaxonomyItem[]>("/api/v1/education-levels");
}

export function fetchLicenses(): Promise<LicenseItem[]> {
  return apiGet<LicenseItem[]>("/api/v1/licenses");
}

export function fetchPublicContent(
  subjectSlug?: string,
  kind?: string,
): Promise<ContentDetail[]> {
  const params = new URLSearchParams();
  if (subjectSlug) {
    params.set("subject", subjectSlug);
  }
  if (kind) {
    params.set("kind", kind);
  }
  const query = params.toString();
  return apiGet<ContentDetail[]>(`/api/v1/public/content${query ? `?${query}` : ""}`);
}

export function fetchPublicBySlug(slug: string): Promise<ContentDetail> {
  return apiGet<ContentDetail>(`/api/v1/public/content/${encodeURIComponent(slug)}`);
}

export function searchPublic(q: string): Promise<SearchPage> {
  return apiGet<SearchPage>(`/api/v1/public/search?q=${encodeURIComponent(q)}`);
}

export function fetchMyContent(): Promise<ContentDetail[]> {
  return apiGet<ContentDetail[]>("/api/v1/my/content");
}

export function fetchContent(id: string): Promise<ContentDetail> {
  return apiGet<ContentDetail>(`/api/v1/content/${id}`);
}

export function fetchRevisions(id: string): Promise<ContentRevision[]> {
  return apiGet<ContentRevision[]>(`/api/v1/my/content/${id}/revisions`);
}

export function createContent(draft: ContentDraft): Promise<ContentDetail> {
  return apiRequest<ContentDetail>("/api/v1/content", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(draft),
  });
}

export function updateContent(id: string, draft: ContentDraft): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(`/api/v1/content/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(draft),
  });
}

export function submitContent(id: string): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(`/api/v1/content/${id}/submit`, { method: "POST" });
}

export function publishContent(id: string): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(`/api/v1/content/${id}/publish`, { method: "POST" });
}

export function archiveContent(id: string): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(`/api/v1/content/${id}/archive`, { method: "POST" });
}

export function reportContent(
  id: string,
  reason: string,
  description: string,
): Promise<{ id: string; status: string }> {
  return apiRequest(`/api/v1/content/${id}/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason, description }),
  });
}

export function fetchReviewQueue(): Promise<Submission[]> {
  return apiGet<Submission[]>("/api/v1/reviews/my");
}

export function fetchReview(id: string): Promise<ContentDetail> {
  return apiGet<ContentDetail>(`/api/v1/reviews/${id}`);
}

export function startReview(id: string): Promise<ContentReview> {
  return apiRequest<ContentReview>(`/api/v1/reviews/${id}/start`, { method: "POST" });
}

export function approveReview(id: string, note: string): Promise<ContentReview> {
  return apiRequest<ContentReview>(`/api/v1/reviews/${id}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ note }),
  });
}

export function requestChanges(id: string, note: string): Promise<ContentReview> {
  return apiRequest<ContentReview>(`/api/v1/reviews/${id}/request-changes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ note }),
  });
}
