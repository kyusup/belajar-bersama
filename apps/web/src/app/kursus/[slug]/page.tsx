"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { fetchCurrentUser } from "@/lib/api/auth";
import { contentHref, fetchPublicBySlug, type ContentDetail } from "@/lib/api/content";
import { fetchProgress, type Progress } from "@/lib/api/learning";
import { CourseOutline } from "@/components/CourseOutline";
import { ProgressBar } from "@/components/ProgressBar";
import { ContentBodyView } from "@/components/ContentBodyView";
import { copy } from "@/lib/i18n/id";

export default function CoursePage() {
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const [content, setContent] = useState<ContentDetail | null>(null);
  const [progress, setProgress] = useState<Progress | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchPublicBySlug(params.slug)
      .then(async (next) => {
        if (next.kind === "QUIZ") {
          router.replace(contentHref(next.kind, next.slug));
          return;
        }
        if (next.kind === "LESSON" || next.kind === "MATERIAL") {
          router.replace(`/materi/${next.slug}`);
          return;
        }
        setContent(next);
        const user = await fetchCurrentUser();
        if (user) {
          setProgress(await fetchProgress(next.id));
        }
      })
      .catch(() => setError(true));
  }, [params.slug, router]);

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
      {progress ? (
        <ProgressBar
          percent={progress.percent}
          completed={progress.completed}
          total={progress.total}
          label={copy.myProgress}
        />
      ) : null}
      <ContentBodyView body={revision.body} />
      <section>
        <h2>{copy.modules}</h2>
        <CourseOutline items={content.children ?? []} />
      </section>
    </article>
  );
}
