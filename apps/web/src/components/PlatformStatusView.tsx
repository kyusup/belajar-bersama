"use client";

import type { PlatformStatus } from "@belajar-bersama/shared";
import { copy } from "@/lib/i18n/id";

type Props = {
  status: PlatformStatus | null;
  error: string | null;
  loading: boolean;
  onRetry: () => void;
};

function badge(value?: string | null) {
  return value === "UP" ? "ok" : "down";
}

export function PlatformStatusView({ status, error, loading, onRetry }: Props) {
  if (loading) {
    return <p className="muted">{copy.loading}</p>;
  }

  if (error || !status) {
    return (
      <section className="panel">
        <p className="error">{copy.apiUnreachable}</p>
        <button type="button" onClick={onRetry}>
          {copy.retry}
        </button>
      </section>
    );
  }

  const database = status.components.database;
  const storage = status.components.storage;
  const search = status.components.search;

  return (
    <section className="panel">
      <p className={badge(status.status) === "ok" ? "ok" : "error"}>
        {status.status === "UP" ? copy.apiReachable : copy.apiUnreachable}
      </p>
      <dl className="status-grid">
        <div>
          <dt>{copy.database}</dt>
          <dd>
            {database?.status} ({database?.provider})
          </dd>
        </div>
        <div>
          <dt>{copy.storage}</dt>
          <dd>
            {storage?.status} ({storage?.provider})
          </dd>
        </div>
        <div>
          <dt>{copy.search}</dt>
          <dd>
            {search?.status} ({search?.provider})
          </dd>
        </div>
        <div>
          <dt>{copy.version}</dt>
          <dd>{status.version}</dd>
        </div>
      </dl>
      <button type="button" onClick={onRetry}>
        {copy.retry}
      </button>
    </section>
  );
}
