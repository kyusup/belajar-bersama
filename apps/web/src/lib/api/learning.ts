import { apiGet, apiRequest } from "./client";
import type { ContentDetail } from "./content";

export type Progress = {
  contentId: string;
  completed: number;
  total: number;
  percent: number;
  lessonCompleted: boolean;
};

export type PublicQuiz = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  passingScore: number | null;
  maxAttempts: number | null;
  required: boolean;
  revisionId: string;
  revisionNumber: number;
  questions: PublicQuestion[];
};

export type PublicQuestion = {
  id: string;
  type: string;
  prompt: string;
  difficulty: string;
  reference: string | null;
  options: PublicOption[];
};

export type PublicOption = {
  id: string;
  label: string;
  text: string;
};

export type AttemptReview = {
  id: string;
  prompt: string;
  type: string;
  explanation: string | null;
  correctOptionIds: string[];
  selectedOptionIds: string[];
  correct: boolean;
  options: PublicOption[];
};

export type Attempt = {
  id: string;
  quizId: string;
  quizRevisionId: string;
  status: string;
  scorePercent: number | null;
  passed: boolean | null;
  correctCount: number | null;
  questionCount: number | null;
  startedAt: string;
  submittedAt: string | null;
  answers: Record<string, string[]>;
  review: AttemptReview[];
};

export function fetchProgress(contentId: string): Promise<Progress> {
  return apiGet<Progress>(`/api/v1/me/progress/${contentId}`);
}

export function completeLesson(contentId: string): Promise<Progress> {
  return apiRequest<Progress>(`/api/v1/me/lessons/${contentId}/complete`, {
    method: "POST",
  });
}

export function markOpened(contentId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/me/opened/${contentId}`, { method: "POST" });
}

export function fetchContinue(): Promise<ContentDetail | null> {
  return apiGet<ContentDetail | null>("/api/v1/me/continue");
}

export function fetchBookmarks(): Promise<ContentDetail[]> {
  return apiGet<ContentDetail[]>("/api/v1/me/bookmarks");
}

export function addBookmark(contentId: string): Promise<void> {
  return apiRequest<void>("/api/v1/me/bookmarks", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ contentId }),
  });
}

export function removeBookmark(contentId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/me/bookmarks/${contentId}`, { method: "DELETE" });
}

export function fetchPublicQuiz(slug: string): Promise<PublicQuiz> {
  return apiGet<PublicQuiz>(`/api/v1/public/quizzes/${encodeURIComponent(slug)}`);
}

export function startAttempt(quizId: string): Promise<Attempt> {
  return apiRequest<Attempt>(`/api/v1/me/quizzes/${quizId}/attempts`, {
    method: "POST",
  });
}

export function fetchAttempt(id: string): Promise<Attempt> {
  return apiGet<Attempt>(`/api/v1/me/attempts/${id}`);
}

export function saveAnswers(
  attemptId: string,
  answers: Record<string, string[]>,
): Promise<Attempt> {
  return apiRequest<Attempt>(`/api/v1/me/attempts/${attemptId}/answers`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ answers }),
  });
}

export function submitAttempt(
  attemptId: string,
  answers: Record<string, string[]>,
): Promise<Attempt> {
  return apiRequest<Attempt>(`/api/v1/me/attempts/${attemptId}/submit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ answers }),
  });
}

export function fetchQuizHistory(): Promise<Attempt[]> {
  return apiGet<Attempt[]>("/api/v1/me/quiz-history");
}
