const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function createCorrelationId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `web-${Date.now()}`;
}

export class HttpError extends Error {
  status: number;
  code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "HttpError";
    this.status = status;
    this.code = code;
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const correlationId = createCorrelationId();
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  headers.set("X-Correlation-Id", correlationId);
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers,
    cache: "no-store",
  });
  if (response.status === 204) {
    if (!response.ok) {
      throw new HttpError(response.status, `API_UNREACHABLE:${response.status}`);
    }
    return undefined as T;
  }
  const text = await response.text();
  if (!response.ok) {
    let code: string | undefined;
    try {
      code = (JSON.parse(text) as { code?: string }).code;
    } catch {
      code = undefined;
    }
    throw new HttpError(response.status, `API_UNREACHABLE:${response.status}`, code);
  }
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export async function apiGet<T>(path: string): Promise<T> {
  return apiRequest<T>(path);
}

export function getApiBaseUrl(): string {
  return API_BASE_URL;
}
