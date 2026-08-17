"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  fetchCompetencies,
  fetchCurrentUser,
  type Competency,
  type CurrentUser,
} from "@/lib/api/auth";
import {
  archiveContent,
  fetchContent,
  fetchEducationLevels,
  fetchLicenses,
  fetchRevisions,
  fetchSubjects,
  publishContent,
  submitContent,
  updateContent,
  type ContentDetail,
  type ContentRevision,
  type LicenseItem,
  type TaxonomyItem,
} from "@/lib/api/content";
import { ContentEditor } from "@/components/ContentEditor";
import { StatusBadge } from "@/components/StatusBadge";
import { copy } from "@/lib/i18n/id";

export default function MyContentDetailPage() {
  const params = useParams<{ id: string }>();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [content, setContent] = useState<ContentDetail | null>(null);
  const [revisions, setRevisions] = useState<ContentRevision[]>([]);
  const [subjects, setSubjects] = useState<TaxonomyItem[]>([]);
  const [levels, setLevels] = useState<TaxonomyItem[]>([]);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [licenses, setLicenses] = useState<LicenseItem[]>([]);
  const [error, setError] = useState(false);

  async function reload() {
    const next = await fetchContent(params.id);
    setContent(next);
    setRevisions(await fetchRevisions(params.id));
  }

  useEffect(() => {
    void Promise.all([
      fetchCurrentUser(),
      fetchContent(params.id),
      fetchRevisions(params.id),
      fetchSubjects(),
      fetchEducationLevels(),
      fetchCompetencies(),
      fetchLicenses(),
    ])
      .then(
        ([
          nextUser,
          nextContent,
          nextRevisions,
          nextSubjects,
          nextLevels,
          nextCompetencies,
          nextLicenses,
        ]) => {
          setUser(nextUser);
          setContent(nextContent);
          setRevisions(nextRevisions);
          setSubjects(nextSubjects);
          setLevels(nextLevels);
          setCompetencies(nextCompetencies);
          setLicenses(nextLicenses);
        },
      )
      .catch(() => setError(true));
  }, [params.id]);

  if (error) {
    return <p className="error">{copy.apiUnreachable}</p>;
  }
  if (!content || !user) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }

  const editable =
    content.status === "DRAFT" ||
    content.status === "CHANGES_REQUESTED" ||
    content.status === "PUBLISHED";
  const canSubmit =
    content.status === "DRAFT" || content.status === "CHANGES_REQUESTED";
  const canPublish = content.status === "APPROVED";
  const canArchive =
    content.status !== "ARCHIVED" && user.permissions.includes("CONTENT_ARCHIVE");

  return (
    <article>
      <p>
        <Link href="/konten-saya">{copy.myContent}</Link>
      </p>
      <h1>{content.currentRevision.title}</h1>
      <p>
        <StatusBadge status={content.status} /> · {copy.revision}{" "}
        {content.currentRevisionNumber}
      </p>
      {content.reviews.length > 0 ? (
        <section className="panel">
          <h2>{copy.reviews}</h2>
          <ul>
            {content.reviews.map((review) => (
              <li key={review.id}>
                {review.decision ?? "—"} — {review.comment} ({review.createdAt})
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      <p>
        {canSubmit ? (
          <button
            type="button"
            onClick={() =>
              void submitContent(params.id)
                .then(setContent)
                .catch(() => setError(true))
            }
          >
            {copy.submitReview}
          </button>
        ) : null}{" "}
        {canPublish ? (
          <button
            type="button"
            onClick={() =>
              void publishContent(params.id)
                .then(setContent)
                .catch(() => setError(true))
            }
          >
            {copy.publish}
          </button>
        ) : null}{" "}
        {canArchive ? (
          <button
            type="button"
            onClick={() =>
              void archiveContent(params.id)
                .then(setContent)
                .catch(() => setError(true))
            }
          >
            {copy.archive}
          </button>
        ) : null}
      </p>
      {editable ? (
        <ContentEditor
          initial={{
            kind: content.kind,
            title: content.currentRevision.title,
            summary: content.currentRevision.summary,
            subjectId: content.subjectId,
            educationLevelId: content.educationLevelId,
            parentId: content.parentId,
            sortOrder: content.sortOrder,
            required: content.required,
            competencyIds: content.currentRevision.competencyIds,
            license: content.currentRevision.license,
            body: content.currentRevision.body,
            sources: content.currentRevision.sources,
            quiz: content.quiz,
          }}
          subjects={subjects}
          levels={levels}
          competencies={competencies}
          licenses={licenses}
          verifiedCompetencyIds={user.verifiedCompetencyIds}
          submitLabel={copy.editDraft}
          onSubmit={async (draft) => {
            await updateContent(params.id, draft);
            await reload();
          }}
        />
      ) : (
        <p className="muted">Materi yang sedang ditinjau tidak dapat diubah.</p>
      )}
      <section>
        <h2>{copy.revisionHistory}</h2>
        <ul>
          {revisions.map((revision) => (
            <li key={revision.id}>
              #{revision.revisionNumber} {revision.title} ({revision.createdAt})
            </li>
          ))}
        </ul>
      </section>
    </article>
  );
}
