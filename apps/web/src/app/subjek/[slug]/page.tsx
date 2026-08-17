"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  contentHref,
  fetchPublicContent,
  fetchSubjects,
  type ContentDetail,
} from "@/lib/api/content";
import { copy } from "@/lib/i18n/id";

export default function SubjectDetailPage() {
  const params = useParams<{ slug: string }>();
  const [name, setName] = useState(params.slug);
  const [description, setDescription] = useState<string | null>(null);
  const [paths, setPaths] = useState<ContentDetail[]>([]);
  const [courses, setCourses] = useState<ContentDetail[]>([]);
  const [items, setItems] = useState<ContentDetail[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void Promise.all([
      fetchSubjects(),
      fetchPublicContent(params.slug, "LEARNING_PATH"),
      fetchPublicContent(params.slug, "COURSE"),
      fetchPublicContent(params.slug),
    ])
      .then(([subjects, nextPaths, nextCourses, nextItems]) => {
        const subject = subjects.find((item) => item.slug === params.slug);
        setName(subject?.name ?? params.slug);
        setDescription(subject?.description ?? null);
        setPaths(nextPaths);
        setCourses(nextCourses);
        setItems(nextItems);
      })
      .catch(() => setError(true));
  }, [params.slug]);

  return (
    <article>
      <p>
        <Link href="/subjek">{copy.subjects}</Link>
      </p>
      <h1>{name}</h1>
      {description ? <p>{description}</p> : null}
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      <section>
        <h2>{copy.learningPaths}</h2>
        {paths.length === 0 ? (
          <p className="muted">{copy.noPublished}</p>
        ) : (
          <ul className="card-list">
            {paths.map((item) => (
              <li key={item.id}>
                <Link href={contentHref(item.kind, item.slug)}>
                  {item.currentRevision.title}
                </Link>
                <p className="muted">{item.currentRevision.summary}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section>
        <h2>{copy.courses}</h2>
        {courses.length === 0 ? (
          <p className="muted">{copy.noPublished}</p>
        ) : (
          <ul className="card-list">
            {courses.map((item) => (
              <li key={item.id}>
                <Link href={contentHref(item.kind, item.slug)}>
                  {item.currentRevision.title}
                </Link>
                <p className="muted">{item.currentRevision.summary}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section>
        <h2>{copy.readMore}</h2>
        {items.length === 0 ? (
          <p className="muted">{copy.noPublished}</p>
        ) : (
          <ul className="card-list">
            {items.map((item) => (
              <li key={item.id}>
                <Link href={contentHref(item.kind, item.slug)}>
                  {item.currentRevision.title}
                </Link>
                <p className="muted">{item.currentRevision.summary}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </article>
  );
}
