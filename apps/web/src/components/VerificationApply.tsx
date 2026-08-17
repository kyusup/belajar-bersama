"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { fetchCompetencies, fetchCurrentUser, type Competency } from "@/lib/api/auth";
import {
  applyVerification,
  fetchMyVerifications,
  type Verification,
} from "@/lib/api/admin";
import { copy } from "@/lib/i18n/id";

export function VerificationApply() {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [items, setItems] = useState<Verification[]>([]);
  const [competencyId, setCompetencyId] = useState("");
  const [qualification, setQualification] = useState("");
  const [experience, setExperience] = useState("");
  const [error, setError] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    void Promise.all([
      fetchCurrentUser(),
      fetchCompetencies(),
      fetchMyVerifications().catch(() => [] as Verification[]),
    ])
      .then(([user, nextCompetencies, mine]) => {
        setSignedIn(Boolean(user));
        setCompetencies(nextCompetencies);
        setItems(mine);
        setCompetencyId((current) => current || nextCompetencies[0]?.id || "");
      })
      .catch(() => {
        setError(true);
        setSignedIn(false);
      });
  }, []);

  if (signedIn === null) {
    return <p className="muted">{copy.loadingAccount}</p>;
  }
  if (!signedIn) {
    return null;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSaved(false);
    try {
      await applyVerification({ competencyId, qualification, experience });
      setQualification("");
      setExperience("");
      setSaved(true);
      setItems(await fetchMyVerifications());
    } catch {
      setError(true);
    }
  }

  return (
    <section className="panel stack editor">
      <h2>{copy.applyVerification}</h2>
      <p className="muted">{copy.applyVerificationIntro}</p>
      {error ? <p className="error">{copy.saveError}</p> : null}
      {saved ? <p>{copy.verificationSubmitted}</p> : null}
      <form className="stack editor" onSubmit={(event) => void onSubmit(event)}>
        <label>
          {copy.competency}
          <select
            value={competencyId}
            onChange={(event) => setCompetencyId(event.target.value)}
          >
            {competencies.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          {copy.qualification}
          <textarea
            required
            value={qualification}
            onChange={(event) => setQualification(event.target.value)}
          />
        </label>
        <label>
          {copy.experience}
          <textarea
            value={experience}
            onChange={(event) => setExperience(event.target.value)}
          />
        </label>
        <button type="submit">{copy.applyVerification}</button>
      </form>
      {items.length > 0 ? (
        <ul className="card-list">
          {items.map((item) => (
            <li key={item.id}>
              {competencies.find((competency) => competency.id === item.competencyId)
                ?.name || item.competencyId}
              {" · "}
              {item.status}
            </li>
          ))}
        </ul>
      ) : null}
      <p>
        <Link href="/konten-saya">{copy.myContent}</Link>
      </p>
    </section>
  );
}
