import type { HealthResponse, PlatformStatus } from "@belajar-bersama/shared";
import { apiGet } from "./client";

export function fetchHealth(): Promise<HealthResponse> {
  return apiGet<HealthResponse>("/api/v1/health");
}

export function fetchPlatformStatus(): Promise<PlatformStatus> {
  return apiGet<PlatformStatus>("/api/v1/status");
}
