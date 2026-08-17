"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchCurrentUser, type CurrentUser } from "@/lib/api/auth";
import { fetchMyContent, type ContentDetail } from "@/lib/api/content";
import { StatusBadge } from "@/components/StatusBadge";
import { copy } from "@/lib/i18n/id";

export default function MyContentPage() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [items, setItems] = useState<ContentDetail[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchCurrentUser()
      .then(async (nextUser) => {
        setUser(nextUser);
        if (nextUser?.permissions.includes("CONTENT_CREATE")) {
          setItems(await fetchMyContent());
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
  if (!user.permissions.includes("CONTENT_CREATE")) {
    return <p>{copy.cannotCreate}</p>;
  }

  return (
    <article>
      <h1>{copy.myContent}</h1>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      <p>
        <Link className="button" href="/konten-saya/baru">
          {copy.createContent}
        </Link>
      </p>
      {items.length === 0 ? (
        <p className="muted">{copy.noContent}</p>
      ) : (
        <ul className="card-list">
          {items.map((item) => (
            <li key={item.id}>
              <Link href={`/konten-saya/${item.id}`}>{item.currentRevision.title}</Link>{" "}
              <StatusBadge status={item.status} />
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
