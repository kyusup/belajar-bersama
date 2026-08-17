import { apiGet, apiRequest } from "./client";

export type QaAnswer = {
  id: string;
  questionId: string;
  authorId: string;
  authorDisplayName: string;
  body: string;
  accepted: boolean;
  usefulCount: number;
  markedUseful: boolean;
  createdAt: string;
};

export type QaQuestion = {
  id: string;
  title: string;
  body: string;
  authorId: string;
  authorDisplayName: string;
  subjectId: string | null;
  contentId: string | null;
  status: string;
  acceptedAnswerId: string | null;
  createdAt: string;
  updatedAt: string;
  answers: QaAnswer[];
};

export type QaPage = {
  items: QaQuestion[];
  page: number;
  size: number;
  totalItems: number;
};

export type QaReport = {
  id: string;
  targetType: string;
  targetId: string;
  reason: string;
  description: string;
  status: string;
  createdAt: string;
};

export type ContentReportItem = {
  id: string;
  contentId: string;
  reporterId: string;
  reason: string;
  description: string;
  status: string;
  createdAt: string;
};

export function fetchPublicQa(contentId?: string): Promise<QaPage> {
  const params = new URLSearchParams();
  if (contentId) {
    params.set("contentId", contentId);
  }
  const query = params.toString();
  return apiGet<QaPage>(`/api/v1/public/qa${query ? `?${query}` : ""}`);
}

export function fetchPublicQuestion(id: string): Promise<QaQuestion> {
  return apiGet<QaQuestion>(`/api/v1/public/qa/${id}`);
}

export function askQuestion(input: {
  title: string;
  body: string;
  subjectId?: string;
  contentId?: string;
}): Promise<QaQuestion> {
  return apiRequest<QaQuestion>("/api/v1/qa", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function answerQuestion(questionId: string, body: string): Promise<QaQuestion> {
  return apiRequest<QaQuestion>(`/api/v1/qa/${questionId}/answers`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ body }),
  });
}

export function closeQuestion(id: string): Promise<QaQuestion> {
  return apiRequest<QaQuestion>(`/api/v1/qa/${id}/close`, { method: "POST" });
}

export function acceptAnswer(
  questionId: string,
  answerId: string,
): Promise<QaQuestion> {
  return apiRequest<QaQuestion>(`/api/v1/qa/${questionId}/accept/${answerId}`, {
    method: "POST",
  });
}

export function unacceptAnswer(questionId: string): Promise<QaQuestion> {
  return apiRequest<QaQuestion>(`/api/v1/qa/${questionId}/accept`, {
    method: "DELETE",
  });
}

export function markUseful(answerId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/qa/answers/${answerId}/useful`, { method: "POST" });
}

export function unmarkUseful(answerId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/qa/answers/${answerId}/useful`, {
    method: "DELETE",
  });
}

export function reportQuestion(
  id: string,
  reason: string,
  description: string,
): Promise<QaReport> {
  return apiRequest<QaReport>(`/api/v1/qa/${id}/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason, description }),
  });
}

export function reportAnswer(
  id: string,
  reason: string,
  description: string,
): Promise<QaReport> {
  return apiRequest<QaReport>(`/api/v1/qa/answers/${id}/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason, description }),
  });
}

export function fetchQaReports(): Promise<QaReport[]> {
  return apiGet<QaReport[]>("/api/v1/moderation/reports");
}

export function fetchContentReports(): Promise<ContentReportItem[]> {
  return apiGet<ContentReportItem[]>("/api/v1/moderation/content-reports");
}

export function hideQuestion(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/moderation/qa/${id}/hide`, { method: "POST" });
}

export function hideAnswer(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/moderation/qa/answers/${id}/hide`, {
    method: "POST",
  });
}

export function resolveQaReport(id: string): Promise<QaReport> {
  return apiRequest<QaReport>(`/api/v1/moderation/reports/${id}/resolve`, {
    method: "POST",
  });
}

export function dismissQaReport(id: string): Promise<QaReport> {
  return apiRequest<QaReport>(`/api/v1/moderation/reports/${id}/dismiss`, {
    method: "POST",
  });
}

export function resolveContentReport(id: string): Promise<ContentReportItem> {
  return apiRequest<ContentReportItem>(
    `/api/v1/moderation/content-reports/${id}/resolve`,
    {
      method: "POST",
    },
  );
}

export function dismissContentReport(id: string): Promise<ContentReportItem> {
  return apiRequest<ContentReportItem>(
    `/api/v1/moderation/content-reports/${id}/dismiss`,
    {
      method: "POST",
    },
  );
}
