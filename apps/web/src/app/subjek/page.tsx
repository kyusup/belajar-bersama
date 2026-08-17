"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { fetchSubjects, type TaxonomyItem } from "@/lib/api/content";
import { copy } from "@/lib/i18n/id";

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState<TaxonomyItem[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchSubjects()
      .then(setSubjects)
      .catch(() => setError(true));
  }, []);

  return (
    <article>
      <h1>{copy.subjects}</h1>
      {error ? <p className="error">{copy.apiUnreachable}</p> : null}
      <ul className="card-list">
        {subjects.map((subject) => (
          <li key={subject.id}>
            <Link href={`/subjek/${subject.slug}`}>{subject.name}</Link>
            {subject.description ? (
              <p className="muted">{subject.description}</p>
            ) : null}
          </li>
        ))}
      </ul>
    </article>
  );
}
