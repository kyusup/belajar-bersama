import { apiGet, apiRequest } from "./client";

export type Verification = {
  id: string;
  applicantId: string;
  competencyId: string;
  status: string;
  qualification: string | null;
  experience: string | null;
  reviewerId: string | null;
  decisionNote: string | null;
  decidedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export function fetchMyVerifications(): Promise<Verification[]> {
  return apiGet<Verification[]>("/api/v1/verifications/me");
}

export function applyVerification(input: {
  competencyId: string;
  qualification: string;
  experience?: string;
}): Promise<Verification> {
  return apiRequest<Verification>("/api/v1/verifications", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function fetchPendingVerifications(): Promise<Verification[]> {
  return apiGet<Verification[]>("/api/v1/admin/verifications");
}

export function decideVerification(
  id: string,
  action: "approve" | "reject" | "request-changes" | "revoke",
  note: string,
): Promise<Verification> {
  return apiRequest<Verification>(`/api/v1/admin/verifications/${id}/${action}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ note }),
  });
}

export function assignRole(userId: string, role: string): Promise<void> {
  return apiRequest<void>(`/api/v1/admin/users/${userId}/roles`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role }),
  });
}

export function revokeRole(userId: string, role: string): Promise<void> {
  return apiRequest<void>(`/api/v1/admin/users/${userId}/roles/${role}`, {
    method: "DELETE",
  });
}

export function createSubject(
  name: string,
  description: string,
): Promise<{ id: string; name: string; slug: string }> {
  return apiRequest(`/api/v1/admin/subjects`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, description }),
  });
}

export function createEducationLevel(
  name: string,
  sortOrder: number,
): Promise<{ id: string; name: string; slug: string }> {
  return apiRequest(`/api/v1/admin/education-levels`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, sortOrder }),
  });
}

export type AdminUser = {
  id: string;
  displayName: string;
  status: string;
  createdAt: string;
  storedRoles: string[];
};

export type AdminUserPage = {
  items: AdminUser[];
  page: number;
  size: number;
  totalItems: number;
};

export function fetchAdminUsers(query?: string): Promise<AdminUserPage> {
  const params = new URLSearchParams();
  if (query && query.trim()) {
    params.set("q", query.trim());
  }
  const suffix = params.toString();
  return apiGet<AdminUserPage>(`/api/v1/admin/users${suffix ? `?${suffix}` : ""}`);
}

export function suspendUser(userId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/admin/users/${userId}/suspend`, { method: "POST" });
}

export function reactivateUser(userId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/admin/users/${userId}/reactivate`, {
    method: "POST",
  });
}
