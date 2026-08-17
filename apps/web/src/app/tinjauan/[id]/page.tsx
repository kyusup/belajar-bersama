"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { fetchCurrentUser, type CurrentUser } from "@/lib/api/auth";
import {
  approveReview,
  fetchReview,
  requestChanges,
  startReview,
  type ContentDetail,
} from "@/lib/api/content";
import { ContentBodyView } from "@/components/ContentBodyView";
import { StatusBadge } from "@/components/StatusBadge";
import { copy } from "@/lib/i18n/id";

export default function ReviewDetailPage() {
  const params = useParams<{ id: string }>();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [content, setContent] = useState<ContentDetail | null>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState(false);

  async function reload() {
    setContent(await fetchReview(params.id));
  }

  useEffect(() => {
    void Promise.all([fetchCurrentUser(), fetchReview(params.id)])
      .then(([nextUser, nextContent]) => {
        setUser(nextUser);
        setContent(nextContent);
      })
      .catch(() => setError(true));
  }, [params.id]);

  if (error) {
    return <p className="error">{copy.apiUnreachable}</p>;
  }
  if (!content || !user) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }
  if (!user.permissions.includes("CONTENT_REVIEW") || user.id === content.makerId) {
    return <p>Anda tidak dapat meninjau materi ini.</p>;
  }

  const revision = content.currentRevision;
  const inReview = content.status === "IN_REVIEW" || content.status === "SUBMITTED";

  return (
    <article>
      <p>
        <Link href="/tinjauan">{copy.reviewQueue}</Link>
      </p>
      <h1>{revision.title}</h1>
      <p>
        <StatusBadge status={content.status} /> · {copy.maker}:{" "}
        {content.makerDisplayName} · {copy.revision} {revision.revisionNumber} ·{" "}
        {content.subjectName}
      </p>
      <p>{revision.summary}</p>
      <ContentBodyView body={revision.body} />
      {revision.sources.length > 0 ? (
        <section>
          <h2>{copy.sources}</h2>
          <ul>
            {revision.sources.map((source) => (
              <li key={source.id ?? source.title}>{source.title}</li>
            ))}
          </ul>
        </section>
      ) : null}
      {content.reviews.length > 0 ? (
        <section>
          <h2>{copy.reviews}</h2>
          <ul>
            {content.reviews.map((review) => (
              <li key={review.id}>
                {review.decision ?? "berjalan"} — {review.comment}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      {inReview ? (
        <section className="panel stack">
          <label>
            {copy.reviewComment}
            <textarea value={note} onChange={(event) => setNote(event.target.value)} />
          </label>
          {content.status === "SUBMITTED" ? (
            <button
              type="button"
              onClick={() => void startReview(params.id).then(() => reload())}
            >
              {copy.startReview}
            </button>
          ) : (
            <p>
              <button
                type="button"
                onClick={() => void approveReview(params.id, note).then(() => reload())}
              >
                {copy.approve}
              </button>{" "}
              <button
                type="button"
                onClick={() =>
                  void requestChanges(params.id, note).then(() => reload())
                }
              >
                {copy.requestChanges}
              </button>
            </p>
          )}
        </section>
      ) : null}
    </article>
  );
}
