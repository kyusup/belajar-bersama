"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { fetchCurrentUser } from "@/lib/api/auth";
import {
  contentHref,
  fetchPublicBySlug,
  fetchPublicContent,
  reportContent,
  type ContentDetail,
} from "@/lib/api/content";
import {
  addBookmark,
  completeLesson,
  fetchBookmarks,
  fetchProgress,
  markOpened,
  removeBookmark,
} from "@/lib/api/learning";
import { ContentBodyView } from "@/components/ContentBodyView";
import { CourseOutline } from "@/components/CourseOutline";
import { copy } from "@/lib/i18n/id";

export default function MateriPage() {
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const [content, setContent] = useState<ContentDetail | null>(null);
  const [siblings, setSiblings] = useState<ContentDetail[]>([]);
  const [signedIn, setSignedIn] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [bookmarked, setBookmarked] = useState(false);
  const [reason, setReason] = useState("INCORRECT");
  const [description, setDescription] = useState("");
  const [reported, setReported] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    void Promise.all([fetchPublicBySlug(params.slug), fetchCurrentUser()])
      .then(async ([next, user]) => {
        if (next.kind === "COURSE" || next.kind === "LEARNING_PATH") {
          router.replace(contentHref(next.kind, next.slug));
          return;
        }
        if (next.kind === "QUIZ") {
          router.replace(contentHref(next.kind, next.slug));
          return;
        }
        setContent(next);
        setSignedIn(Boolean(user));
        if (next.parentId) {
          const catalog = await fetchPublicContent();
          setSiblings(
            catalog
              .filter((item) => item.parentId === next.parentId)
              .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0)),
          );
        }
        if (user) {
          await markOpened(next.id);
          const [marks, progress] = await Promise.all([
            fetchBookmarks(),
            fetchProgress(next.id),
          ]);
          setBookmarked(marks.some((item) => item.id === next.id));
          setCompleted(progress.lessonCompleted);
        }
      })
      .catch(() => setError(true));
  }, [params.slug, router]);

  async function onComplete() {
    if (!content) {
      return;
    }
    try {
      const progress = await completeLesson(content.id);
      setCompleted(progress.lessonCompleted);
    } catch {
      setError(true);
    }
  }

  async function onBookmark() {
    if (!content) {
      return;
    }
    try {
      if (bookmarked) {
        await removeBookmark(content.id);
        setBookmarked(false);
      } else {
        await addBookmark(content.id);
        setBookmarked(true);
      }
    } catch {
      setError(true);
    }
  }

  async function onReport(event: React.FormEvent) {
    event.preventDefault();
    if (!content) {
      return;
    }
    try {
      await reportContent(content.id, reason, description);
      setReported(true);
    } catch {
      setError(true);
    }
  }

  if (error && !content) {
    return <p className="error">{copy.apiUnreachable}</p>;
  }
  if (!content) {
    return <p className="muted">{copy.loading}</p>;
  }

  const revision = content.currentRevision;
  return (
    <article>
      <p className="muted">
        {content.subjectName} · {content.educationLevelName}
      </p>
      <h1>{revision.title}</h1>
      <p className="muted">
        {copy.contributor}: {content.makerDisplayName}
      </p>
      <p>{revision.summary}</p>
      <ContentBodyView body={revision.body} />
      {content.children && content.children.length > 0 ? (
        <section>
          <h2>{copy.lessons}</h2>
          <CourseOutline items={content.children} />
        </section>
      ) : null}
      <p>
        {copy.license}: {revision.license}
      </p>
      {revision.sources.length > 0 ? (
        <section>
          <h2>{copy.sources}</h2>
          <ul>
            {revision.sources.map((source) => (
              <li key={source.id ?? source.title}>
                {source.title}
                {source.author ? ` — ${source.author}` : ""}
                {source.url ? (
                  <>
                    {" "}
                    <a href={source.url} rel="noopener noreferrer">
                      {source.url}
                    </a>
                  </>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      <nav className="lesson-nav" aria-label="Navigasi pelajaran">
        {(() => {
          const index = siblings.findIndex((item) => item.id === content.id);
          const previous = index > 0 ? siblings[index - 1] : null;
          const nextItem =
            index >= 0 && index < siblings.length - 1 ? siblings[index + 1] : null;
          return (
            <>
              {previous ? (
                <Link href={contentHref(previous.kind, previous.slug)}>
                  {copy.previous}
                </Link>
              ) : (
                <span />
              )}
              {nextItem ? (
                <Link href={contentHref(nextItem.kind, nextItem.slug)}>
                  {copy.next}
                </Link>
              ) : (
                <span />
              )}
            </>
          );
        })()}
      </nav>
      <p>
        <Link href={`/tanya?content=${content.id}`}>{copy.qaAboutLesson}</Link>
      </p>
      <section className="panel stack">
        {signedIn ? (
          <>
            <button
              type="button"
              onClick={() => void onComplete()}
              disabled={completed}
            >
              {completed ? copy.completed : copy.markComplete}
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => void onBookmark()}
            >
              {bookmarked ? copy.removeBookmark : copy.addBookmark}
            </button>
          </>
        ) : (
          <p>
            {copy.needLoginLearn} <Link href="/masuk">{copy.login}</Link>
          </p>
        )}
      </section>
      <section className="panel">
        <h2>{copy.report}</h2>
        {!signedIn ? (
          <p>
            {copy.reportNeedLogin} <Link href="/masuk">{copy.login}</Link>
          </p>
        ) : reported ? (
          <p>{copy.reportThanks}</p>
        ) : (
          <form className="stack" onSubmit={(event) => void onReport(event)}>
            <label>
              {copy.reportReason}
              <select
                value={reason}
                onChange={(event) => setReason(event.target.value)}
              >
                <option value="INCORRECT">Informasi tidak tepat</option>
                <option value="COPYRIGHT">Hak cipta</option>
                <option value="INAPPROPRIATE">Tidak pantas</option>
                <option value="SPAM">Spam</option>
                <option value="OTHER">Lainnya</option>
              </select>
            </label>
            <label>
              {copy.reportDescription}
              <textarea
                required
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
            <button type="submit">{copy.reportSubmit}</button>
          </form>
        )}
      </section>
    </article>
  );
}
