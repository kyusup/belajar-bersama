"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { fetchCurrentUser } from "@/lib/api/auth";
import { askQuestion, fetchPublicQa, type QaQuestion } from "@/lib/api/qa";
import { copy } from "@/lib/i18n/id";

export function TanyaBoard() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const contentId = searchParams.get("content") ?? undefined;
  const [items, setItems] = useState<QaQuestion[]>([]);
  const [signedIn, setSignedIn] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void Promise.all([fetchPublicQa(contentId), fetchCurrentUser()])
      .then(([page, user]) => {
        setItems(page.items);
        setSignedIn(Boolean(user));
      })
      .catch(() => setError(true));
  }, [contentId]);

  async function onAsk(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    try {
      const created = await askQuestion({ title, body, contentId });
      router.push(`/tanya/${created.id}`);
    } catch {
      setError(true);
      setSaving(false);
    }
  }

  return (
    <article>
      <h1>{copy.qaTitle}</h1>
      <p className="muted">{copy.qaIntro}</p>
      {contentId ? <p className="muted">{copy.qaAboutLesson}</p> : null}
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      {signedIn ? (
        <form className="panel stack editor" onSubmit={(event) => void onAsk(event)}>
          <h2>{copy.qaAsk}</h2>
          <label>
            {copy.title}
            <input
              required
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>
          <label>
            {copy.body}
            <textarea
              required
              value={body}
              onChange={(event) => setBody(event.target.value)}
            />
          </label>
          <button type="submit" disabled={saving}>
            {copy.qaAsk}
          </button>
        </form>
      ) : (
        <p>
          {copy.qaNeedLogin} <Link href="/masuk">{copy.login}</Link>
        </p>
      )}
      <section>
        <h2>{copy.qaList}</h2>
        {items.length === 0 ? (
          <p className="muted">{copy.qaEmpty}</p>
        ) : (
          <ul className="card-list">
            {items.map((item) => (
              <li key={item.id}>
                <Link href={`/tanya/${item.id}`}>{item.title}</Link>
                <p className="muted">
                  {item.authorDisplayName} ·{" "}
                  {item.status === "CLOSED" ? copy.qaClosed : copy.qaOpen}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </article>
  );
}
