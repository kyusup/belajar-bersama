"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { fetchAttempt, type Attempt } from "@/lib/api/learning";
import { copy } from "@/lib/i18n/id";

export default function QuizResultPage() {
  const params = useParams<{ slug: string; attemptId: string }>();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    void fetchAttempt(params.attemptId)
      .then(setAttempt)
      .catch(() => setError(true));
  }, [params.attemptId]);

  if (error) {
    return <p className="error">{copy.apiUnreachable}</p>;
  }
  if (!attempt) {
    return <p className="muted">{copy.loading}</p>;
  }

  const passLabel =
    attempt.passed == null ? null : attempt.passed ? copy.passed : copy.failed;

  return (
    <article>
      <h1>{copy.resultTitle}</h1>
      <p>
        {copy.score}: {attempt.scorePercent ?? 0}%
      </p>
      {passLabel ? <p>{passLabel}</p> : null}
      <p>
        {copy.correct}: {attempt.correctCount ?? 0} · {copy.incorrect}:{" "}
        {(attempt.questionCount ?? 0) - (attempt.correctCount ?? 0)}
      </p>
      <section>
        <h2>{copy.reviewMistakes}</h2>
        {attempt.review.map((question) => {
          const selected = question.options.filter((option) =>
            question.selectedOptionIds.includes(option.id),
          );
          const correct = question.options.filter((option) =>
            question.correctOptionIds.includes(option.id),
          );
          return (
            <section key={question.id} className="panel">
              <h3>{question.prompt}</h3>
              <p>{question.correct ? copy.correct : copy.incorrect}</p>
              <p>
                {copy.yourAnswer}:{" "}
                {selected.length
                  ? selected
                      .map((option) => `${option.label}. ${option.text}`)
                      .join(", ")
                  : "—"}
              </p>
              <p>
                {copy.correctAnswer}:{" "}
                {correct.map((option) => `${option.label}. ${option.text}`).join(", ")}
              </p>
              {question.explanation ? (
                <p>
                  {copy.explanation}: {question.explanation}
                </p>
              ) : null}
            </section>
          );
        })}
      </section>
      <p>
        <Link href={`/kuis/${params.slug}`}>{copy.tryAgain}</Link>
      </p>
    </article>
  );
}
