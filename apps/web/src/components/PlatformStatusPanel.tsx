"use client";

import { useCallback, useEffect, useState } from "react";
import type { PlatformStatus } from "@belajar-bersama/shared";
import { fetchPlatformStatus } from "@/lib/api/health";
import { PlatformStatusView } from "@/components/PlatformStatusView";

export function PlatformStatusPanel() {
  const [status, setStatus] = useState<PlatformStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await fetchPlatformStatus();
      setStatus(next);
    } catch {
      setStatus(null);
      setError("unreachable");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <PlatformStatusView
      status={status}
      error={error}
      loading={loading}
      onRetry={load}
    />
  );
}
