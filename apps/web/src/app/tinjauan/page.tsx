"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchCurrentUser, type CurrentUser } from "@/lib/api/auth";
import { fetchReviewQueue, type Submission } from "@/lib/api/content";
import { copy } from "@/lib/i18n/id";

export default function ReviewQueuePage() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [items, setItems] = useState<Submission[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchCurrentUser()
      .then(async (nextUser) => {
        setUser(nextUser);
        if (nextUser?.permissions.includes("CONTENT_REVIEW")) {
          setItems(await fetchReviewQueue());
        }
      })
      .catch(() => setError(true));
  }, []);

  if (user === undefined) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }
  if (!user) {
    return (
      <p>
        {copy.notSignedIn} <Link href="/masuk">{copy.login}</Link>
      </p>
    );
  }
  if (!user.permissions.includes("CONTENT_REVIEW")) {
    return <p>Halaman ini hanya untuk Checker.</p>;
  }

  return (
    <article>
      <h1>{copy.reviewQueue}</h1>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      {items.length === 0 ? (
        <p className="muted">{copy.emptyQueue}</p>
      ) : (
        <ul className="card-list">
          {items.map((item) => (
            <li key={item.id}>
              <Link href={`/tinjauan/${item.id}`}>{item.title || item.id}</Link>
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
