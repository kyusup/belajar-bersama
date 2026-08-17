"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchCurrentUser, type CurrentUser } from "@/lib/api/auth";
import {
  dismissContentReport,
  dismissQaReport,
  fetchContentReports,
  fetchQaReports,
  hideAnswer,
  hideQuestion,
  resolveContentReport,
  resolveQaReport,
  type ContentReportItem,
  type QaReport,
} from "@/lib/api/qa";
import { copy } from "@/lib/i18n/id";

export default function ModerasiPage() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [qaReports, setQaReports] = useState<QaReport[]>([]);
  const [contentReports, setContentReports] = useState<ContentReportItem[]>([]);
  const [error, setError] = useState(false);

  async function reload() {
    const [qa, content] = await Promise.all([fetchQaReports(), fetchContentReports()]);
    setQaReports(qa);
    setContentReports(content);
  }

  useEffect(() => {
    void fetchCurrentUser()
      .then(async (nextUser) => {
        setUser(nextUser);
        if (
          nextUser?.permissions.includes("CONTENT_MODERATE") ||
          nextUser?.permissions.includes("CONTENT_REPORT_REVIEW")
        ) {
          await reload();
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
  if (
    !user.permissions.includes("CONTENT_MODERATE") &&
    !user.permissions.includes("CONTENT_REPORT_REVIEW")
  ) {
    return <p>{copy.moderationDenied}</p>;
  }

  return (
    <article>
      <h1>{copy.moderationTitle}</h1>
      <p className="muted">{copy.moderationIntro}</p>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      <section>
        <h2>{copy.qaReports}</h2>
        {qaReports.length === 0 ? (
          <p className="muted">{copy.emptyReports}</p>
        ) : (
          <ul className="card-list">
            {qaReports.map((item) => (
              <li key={item.id}>
                <p>
                  {item.targetType === "QUESTION" ? (
                    <Link href={`/tanya/${item.targetId}`}>{copy.qaQuestionLink}</Link>
                  ) : (
                    <span>{copy.qaAnswerTarget}</span>
                  )}{" "}
                  · {item.reason}
                </p>
                <p>{item.description}</p>
                <p className="stack">
                  {user.permissions.includes("CONTENT_MODERATE") &&
                  item.targetType === "QUESTION" ? (
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => void hideQuestion(item.targetId).then(reload)}
                    >
                      {copy.qaHide}
                    </button>
                  ) : null}
                  {user.permissions.includes("CONTENT_MODERATE") &&
                  item.targetType === "ANSWER" ? (
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => void hideAnswer(item.targetId).then(reload)}
                    >
                      {copy.qaHideAnswer}
                    </button>
                  ) : null}
                  <button
                    type="button"
                    onClick={() => void resolveQaReport(item.id).then(reload)}
                  >
                    {copy.resolveReport}
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => void dismissQaReport(item.id).then(reload)}
                  >
                    {copy.dismissReport}
                  </button>
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section>
        <h2>{copy.contentReports}</h2>
        {contentReports.length === 0 ? (
          <p className="muted">{copy.emptyReports}</p>
        ) : (
          <ul className="card-list">
            {contentReports.map((item) => (
              <li key={item.id}>
                <p>
                  {copy.contentReportTarget}: {item.contentId} · {item.reason}
                </p>
                <p>{item.description}</p>
                <p className="stack">
                  <button
                    type="button"
                    onClick={() => void resolveContentReport(item.id).then(reload)}
                  >
                    {copy.resolveReport}
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => void dismissContentReport(item.id).then(reload)}
                  >
                    {copy.dismissReport}
                  </button>
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </article>
  );
}
