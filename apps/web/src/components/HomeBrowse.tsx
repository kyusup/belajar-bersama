"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  contentHref,
  fetchPublicContent,
  fetchSubjects,
  searchHref,
  searchPublic,
  type ContentDetail,
  type SearchPage,
  type TaxonomyItem,
} from "@/lib/api/content";
import { fetchContinue } from "@/lib/api/learning";
import { fetchCurrentUser } from "@/lib/api/auth";
import { copy } from "@/lib/i18n/id";

export function HomeBrowse() {
  const [subjects, setSubjects] = useState<TaxonomyItem[]>([]);
  const [courses, setCourses] = useState<ContentDetail[]>([]);
  const [items, setItems] = useState<ContentDetail[]>([]);
  const [resume, setResume] = useState<ContentDetail | null>(null);
  const [signedIn, setSignedIn] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchPage | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    void Promise.all([
      fetchSubjects(),
      fetchPublicContent(undefined, "COURSE"),
      fetchPublicContent(),
      fetchCurrentUser(),
    ])
      .then(async ([nextSubjects, nextCourses, nextItems, user]) => {
        setSubjects(nextSubjects);
        setCourses(nextCourses);
        setItems(nextItems);
        setSignedIn(Boolean(user));
        if (user) {
          setResume(await fetchContinue());
        }
      })
      .catch(() => setError(true));
  }, []);

  async function onSearch(event: React.FormEvent) {
    event.preventDefault();
    if (!query.trim()) {
      setResults(null);
      return;
    }
    try {
      setResults(await searchPublic(query.trim()));
    } catch {
      setError(true);
    }
  }

  return (
    <>
      <article className="hero">
        <h1>{copy.productName}</h1>
        <p>{copy.tagline}</p>
        <p className="muted">{copy.foundationNote}</p>
        <p className="muted">{copy.anonymousNote}</p>
        <form className="search-bar" onSubmit={(event) => void onSearch(event)}>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={copy.searchPlaceholder}
            aria-label={copy.search}
          />
          <button type="submit">{copy.searchAction}</button>
        </form>
      </article>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      {signedIn && resume ? (
        <section className="panel">
          <h2>{copy.continueLearning}</h2>
          <p>
            <Link href={contentHref(resume.kind, resume.slug)}>
              {resume.currentRevision.title}
            </Link>
          </p>
        </section>
      ) : null}
      {results ? (
        <section>
          <h2>{copy.search}</h2>
          {results.items.length === 0 ? (
            <p className="muted">{copy.noPublished}</p>
          ) : (
            <ul className="card-list">
              {results.items.map((item) => (
                <li key={item.id}>
                  <Link href={searchHref(item.type, item.slug)}>{item.title}</Link>
                  <p className="muted">{item.summary}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
      <section>
        <h2>{copy.subjects}</h2>
        <ul className="card-list">
          {subjects.map((subject) => (
            <li key={subject.id}>
              <Link href={`/subjek/${subject.slug}`}>{subject.name}</Link>
            </li>
          ))}
        </ul>
      </section>
      <section>
        <h2>{copy.popularCourses}</h2>
        {courses.length === 0 ? (
          <p className="muted">{copy.noPublished}</p>
        ) : (
          <ul className="card-list">
            {courses.map((item) => (
              <li key={item.id}>
                <Link href={contentHref(item.kind, item.slug)}>
                  {item.currentRevision.title}
                </Link>
                <p className="muted">
                  {item.subjectName} · {item.educationLevelName}
                </p>
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
                <p className="muted">
                  {item.subjectName} · {item.educationLevelName}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
