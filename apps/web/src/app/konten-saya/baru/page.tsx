"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  fetchCompetencies,
  fetchCurrentUser,
  type Competency,
  type CurrentUser,
} from "@/lib/api/auth";
import {
  createContent,
  fetchEducationLevels,
  fetchLicenses,
  fetchSubjects,
  type LicenseItem,
  type TaxonomyItem,
} from "@/lib/api/content";
import { ContentEditor } from "@/components/ContentEditor";
import { copy } from "@/lib/i18n/id";

export default function NewContentPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [subjects, setSubjects] = useState<TaxonomyItem[]>([]);
  const [levels, setLevels] = useState<TaxonomyItem[]>([]);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [licenses, setLicenses] = useState<LicenseItem[]>([]);

  useEffect(() => {
    void Promise.all([
      fetchCurrentUser(),
      fetchSubjects(),
      fetchEducationLevels(),
      fetchCompetencies(),
      fetchLicenses(),
    ]).then(([nextUser, nextSubjects, nextLevels, nextCompetencies, nextLicenses]) => {
      setUser(nextUser);
      setSubjects(nextSubjects);
      setLevels(nextLevels);
      setCompetencies(nextCompetencies);
      setLicenses(nextLicenses);
    });
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
  if (!user.permissions.includes("CONTENT_CREATE")) {
    return <p>{copy.cannotCreate}</p>;
  }

  return (
    <article>
      <h1>{copy.createContent}</h1>
      <ContentEditor
        subjects={subjects}
        levels={levels}
        competencies={competencies}
        licenses={licenses}
        verifiedCompetencyIds={user.verifiedCompetencyIds}
        submitLabel={copy.editDraft}
        onSubmit={async (draft) => {
          const created = await createContent(draft);
          router.push(`/konten-saya/${created.id}`);
        }}
      />
    </article>
  );
}
