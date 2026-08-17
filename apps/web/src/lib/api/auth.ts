import { apiGet, apiRequest, getApiBaseUrl, HttpError } from "./client";

export type AuthConfig = {
  google: boolean;
  apple: boolean;
  devLogin: boolean;
};

export type IdentitySummary = {
  id: string;
  provider: string;
  issuer: string;
};

export type CurrentUser = {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  status: string;
  roles: string[];
  storedRoles: string[];
  permissions: string[];
  identities: IdentitySummary[];
  verifiedCompetencyIds: string[];
};

export type Competency = {
  id: string;
  slug: string;
  name: string;
  description: string | null;
};

export function fetchAuthConfig(): Promise<AuthConfig> {
  return apiGet<AuthConfig>("/api/v1/auth/config");
}

export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiGet<CurrentUser>("/api/v1/me");
  } catch (error) {
    if (error instanceof HttpError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export function fetchCompetencies(): Promise<Competency[]> {
  return apiGet<Competency[]>("/api/v1/competencies");
}

export function startProviderLogin(provider: "google" | "apple"): string {
  return `${getApiBaseUrl()}/api/v1/auth/${provider}/start`;
}

export function devLogin(input: {
  provider: "GOOGLE" | "APPLE";
  subject: string;
  displayName: string;
}): Promise<CurrentUser> {
  return apiRequest<CurrentUser>("/api/v1/auth/dev/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function logout(): Promise<void> {
  return apiRequest<void>("/api/v1/auth/logout", { method: "POST" });
}
