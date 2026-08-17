"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchCurrentUser } from "@/lib/api/auth";
import { contentHref, type ContentDetail } from "@/lib/api/content";
import {
  fetchBookmarks,
  fetchContinue,
  fetchQuizHistory,
  type Attempt,
} from "@/lib/api/learning";
import { copy } from "@/lib/i18n/id";

export default function LearnerDashboardPage() {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  const [resume, setResume] = useState<ContentDetail | null>(null);
  const [bookmarks, setBookmarks] = useState<ContentDetail[]>([]);
  const [history, setHistory] = useState<Attempt[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchCurrentUser()
      .then(async (user) => {
        if (!user) {
          setSignedIn(false);
          return;
        }
        setSignedIn(true);
        const [nextResume, nextBookmarks, nextHistory] = await Promise.all([
          fetchContinue(),
          fetchBookmarks(),
          fetchQuizHistory(),
        ]);
        setResume(nextResume);
        setBookmarks(nextBookmarks);
        setHistory(nextHistory);
      })
      .catch(() => setError(true));
  }, []);

  if (signedIn === null && !error) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }
  if (!signedIn) {
    return (
      <p>
        {copy.needLoginLearn} <Link href="/masuk">{copy.login}</Link>
      </p>
    );
  }

  return (
    <article>
      <h1>{copy.learn}</h1>
      <p className="muted">{copy.dashboardIntro}</p>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      <section className="panel">
        <h2>{copy.continueLearning}</h2>
        {resume ? (
          <p>
            <Link href={contentHref(resume.kind, resume.slug)}>
              {resume.currentRevision.title}
            </Link>
          </p>
        ) : (
          <p className="muted">{copy.noContinue}</p>
        )}
      </section>
      <section>
        <h2>{copy.recentQuizzes}</h2>
        {history.length === 0 ? (
          <p className="muted">{copy.noQuizHistory}</p>
        ) : (
          <ul className="card-list">
            {history.map((item) => (
              <li key={item.id}>
                <Link href={`/hasil/${item.id}`}>
                  {copy.score} {item.scorePercent ?? 0}%
                  {item.passed == null
                    ? ""
                    : item.passed
                      ? ` · ${copy.passed}`
                      : ` · ${copy.failed}`}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section>
        <h2>{copy.bookmarks}</h2>
        {bookmarks.length === 0 ? (
          <p className="muted">{copy.noBookmarks}</p>
        ) : (
          <ul className="card-list">
            {bookmarks.map((item) => (
              <li key={item.id}>
                <Link href={contentHref(item.kind, item.slug)}>
                  {item.currentRevision.title}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </article>
  );
}
