"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { fetchCurrentUser } from "@/lib/api/auth";
import { HttpError } from "@/lib/api/client";
import {
  fetchPublicQuiz,
  saveAnswers,
  startAttempt,
  submitAttempt,
  type Attempt,
  type PublicQuiz,
} from "@/lib/api/learning";
import { copy } from "@/lib/i18n/id";

export default function QuizPage() {
  const params = useParams<{ slug: string }>();
  const router = useRouter();
  const [quiz, setQuiz] = useState<PublicQuiz | null>(null);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [index, setIndex] = useState(0);
  const [signedIn, setSignedIn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void Promise.all([fetchPublicQuiz(params.slug), fetchCurrentUser()])
      .then(([nextQuiz, user]) => {
        setQuiz(nextQuiz);
        setSignedIn(Boolean(user));
      })
      .catch(() => setError(copy.apiUnreachable));
  }, [params.slug]);

  async function onStart() {
    if (!quiz) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const next = await startAttempt(quiz.id);
      setAttempt(next);
      setAnswers(next.answers ?? {});
    } catch (caught) {
      if (caught instanceof HttpError && caught.code === "MAX_ATTEMPTS_REACHED") {
        setError(copy.maxAttemptsReached);
      } else if (caught instanceof HttpError && caught.status === 401) {
        setError(copy.quizNeedLogin);
      } else {
        setError(copy.apiUnreachable);
      }
    } finally {
      setBusy(false);
    }
  }

  function toggle(questionId: string, optionId: string, multiple: boolean) {
    setAnswers((current) => {
      const selected = current[questionId] ?? [];
      if (multiple) {
        const next = selected.includes(optionId)
          ? selected.filter((id) => id !== optionId)
          : [...selected, optionId];
        return { ...current, [questionId]: next };
      }
      return { ...current, [questionId]: [optionId] };
    });
  }

  async function persist() {
    if (!attempt) {
      return;
    }
    await saveAnswers(attempt.id, answers);
  }

  async function onSubmit() {
    if (!attempt || !quiz) {
      return;
    }
    setBusy(true);
    try {
      const submitted = await submitAttempt(attempt.id, answers);
      router.push(`/kuis/${quiz.slug}/hasil/${submitted.id}`);
    } catch {
      setError(copy.apiUnreachable);
    } finally {
      setBusy(false);
    }
  }

  if (error && !quiz) {
    return <p className="error">{error}</p>;
  }
  if (!quiz) {
    return <p className="muted">{copy.loading}</p>;
  }

  const question = quiz.questions[index];
  const multiple = question?.type === "MULTIPLE_CHOICE";

  return (
    <article>
      <h1>{quiz.title}</h1>
      <p>{quiz.summary}</p>
      {quiz.passingScore != null ? (
        <p className="muted">
          {copy.passingScore}: {quiz.passingScore}
        </p>
      ) : null}
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}
      {!signedIn ? (
        <p>
          {copy.quizNeedLogin} <Link href="/masuk">{copy.login}</Link>
        </p>
      ) : !attempt ? (
        <button type="button" onClick={() => void onStart()} disabled={busy}>
          {copy.startQuiz}
        </button>
      ) : question ? (
        <form
          className="stack"
          onSubmit={(event) => {
            event.preventDefault();
            void onSubmit();
          }}
        >
          <p className="muted">
            {copy.question} {index + 1} / {quiz.questions.length}
          </p>
          <fieldset className="quiz-fieldset">
            <legend>{question.prompt}</legend>
            <div className="stack">
              {question.options.map((option) => {
                const selected = (answers[question.id] ?? []).includes(option.id);
                return (
                  <label key={option.id} className="quiz-option">
                    <input
                      type={multiple ? "checkbox" : "radio"}
                      name={question.id}
                      value={option.id}
                      checked={selected}
                      onChange={() => toggle(question.id, option.id, multiple)}
                    />
                    <span>
                      <strong>{option.label}.</strong> {option.text}
                    </span>
                  </label>
                );
              })}
            </div>
          </fieldset>
          <div className="lesson-nav">
            <button
              type="button"
              className="secondary"
              disabled={index === 0}
              onClick={() => {
                void persist();
                setIndex((value) => Math.max(0, value - 1));
              }}
            >
              {copy.previous}
            </button>
            {index < quiz.questions.length - 1 ? (
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  void persist();
                  setIndex((value) => Math.min(quiz.questions.length - 1, value + 1));
                }}
              >
                {copy.next}
              </button>
            ) : (
              <button type="submit" disabled={busy}>
                {copy.submitQuiz}
              </button>
            )}
          </div>
        </form>
      ) : null}
    </article>
  );
}
