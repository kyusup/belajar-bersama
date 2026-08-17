"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  fetchCompetencies,
  fetchCurrentUser,
  type Competency,
  type CurrentUser,
} from "@/lib/api/auth";
import { copy } from "@/lib/i18n/id";

export function AccountPanel() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void Promise.all([fetchCurrentUser(), fetchCompetencies()])
      .then(([nextUser, nextCompetencies]) => {
        setUser(nextUser);
        setCompetencies(nextCompetencies);
      })
      .catch(() => {
        setError(true);
        setUser(null);
      });
  }, []);

  if (user === undefined) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }
  if (error) {
    return <p className="error">{copy.apiUnreachable}</p>;
  }
  if (!user) {
    return (
      <p>
        {copy.notSignedIn} <Link href="/masuk">{copy.login}</Link>
      </p>
    );
  }

  const verifiedNames = competencies
    .filter((item) => user.verifiedCompetencyIds.includes(item.id))
    .map((item) => item.name);

  return (
    <section className="panel">
      <div className="profile">
        {user.avatarUrl ? (
          // Provider avatar URLs are not allow-listed in next/image for this phase.
          // eslint-disable-next-line @next/next/no-img-element
          <img className="avatar" src={user.avatarUrl} alt="" />
        ) : (
          <div className="avatar fallback" aria-hidden>
            {user.displayName.slice(0, 1)}
          </div>
        )}
        <div>
          <h2>{user.displayName}</h2>
          <p className="muted">{user.status}</p>
        </div>
      </div>
      <dl className="status-grid">
        <div>
          <dt>{copy.providers}</dt>
          <dd>{user.identities.map((item) => item.provider).join(", ") || "—"}</dd>
        </div>
        <div>
          <dt>{copy.roles}</dt>
          <dd>{user.roles.join(", ")}</dd>
        </div>
        <div>
          <dt>{copy.verifiedCompetencies}</dt>
          <dd>
            {verifiedNames.length > 0
              ? verifiedNames.join(", ")
              : copy.noVerifiedCompetency}
          </dd>
        </div>
      </dl>
      {user.permissions.includes("CONTENT_CREATE") ? (
        <p>
          <Link href="/konten-saya">{copy.myContent}</Link>
        </p>
      ) : null}
      {user.permissions.includes("CONTENT_REVIEW") ? (
        <p>
          <Link href="/tinjauan">{copy.reviews}</Link>
        </p>
      ) : null}
    </section>
  );
}
