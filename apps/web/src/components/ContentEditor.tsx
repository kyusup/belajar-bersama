"use client";

import { useEffect, useState } from "react";
import type {
  ContentDraft,
  LicenseItem,
  QuizQuestionDraft,
  TaxonomyItem,
} from "@/lib/api/content";
import type { Competency } from "@/lib/api/auth";
import { copy } from "@/lib/i18n/id";

type Props = {
  initial?: Partial<ContentDraft>;
  subjects: TaxonomyItem[];
  levels: TaxonomyItem[];
  competencies: Competency[];
  licenses: LicenseItem[];
  verifiedCompetencyIds: string[];
  submitLabel: string;
  onSubmit: (draft: ContentDraft) => Promise<void>;
};

const KINDS = [
  "MATERIAL",
  "LESSON",
  "MODULE",
  "COURSE",
  "LEARNING_PATH",
  "QUIZ",
] as const;

function emptyQuestion(): QuizQuestionDraft {
  return {
    type: "SINGLE_CHOICE",
    prompt: "",
    explanation: "",
    difficulty: "MEDIUM",
    options: [
      { label: "A", text: "", correct: true },
      { label: "B", text: "", correct: false },
    ],
  };
}

export function ContentEditor({
  initial,
  subjects,
  levels,
  competencies,
  licenses,
  verifiedCompetencyIds,
  submitLabel,
  onSubmit,
}: Props) {
  const [kind, setKind] = useState(initial?.kind ?? "MATERIAL");
  const [title, setTitle] = useState(initial?.title ?? "");
  const [summary, setSummary] = useState(initial?.summary ?? "");
  const [subjectId, setSubjectId] = useState(
    initial?.subjectId ?? subjects[0]?.id ?? "",
  );
  const [educationLevelId, setEducationLevelId] = useState(
    initial?.educationLevelId ?? levels[0]?.id ?? "",
  );
  const [parentId, setParentId] = useState(initial?.parentId ?? "");
  const [sortOrder, setSortOrder] = useState(initial?.sortOrder ?? 0);
  const [required, setRequired] = useState(initial?.required ?? true);
  const [competencyIds, setCompetencyIds] = useState<string[]>(
    initial?.competencyIds ??
      (verifiedCompetencyIds[0] ? [verifiedCompetencyIds[0]] : []),
  );
  const [license, setLicense] = useState(initial?.license ?? "CC_BY_SA");
  const [body, setBody] = useState(initial?.body?.blocks?.[0]?.text ?? "");
  const [sourceTitle, setSourceTitle] = useState(initial?.sources?.[0]?.title ?? "");
  const [sourceAuthor, setSourceAuthor] = useState(initial?.sources?.[0]?.author ?? "");
  const [sourceUrl, setSourceUrl] = useState(initial?.sources?.[0]?.url ?? "");
  const [passingScore, setPassingScore] = useState(
    initial?.quiz?.passingScore?.toString() ?? "",
  );
  const [maxAttempts, setMaxAttempts] = useState(
    initial?.quiz?.maxAttempts?.toString() ?? "",
  );
  const [questions, setQuestions] = useState<QuizQuestionDraft[]>(
    initial?.quiz?.questions ?? [emptyQuestion()],
  );
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (initial?.title) {
      setKind(initial.kind ?? "MATERIAL");
      setTitle(initial.title);
      setSummary(initial.summary ?? "");
      setSubjectId(initial.subjectId ?? subjectId);
      setEducationLevelId(initial.educationLevelId ?? educationLevelId);
      setParentId(initial.parentId ?? "");
      setSortOrder(initial.sortOrder ?? 0);
      setRequired(initial.required ?? true);
      setCompetencyIds(initial.competencyIds ?? competencyIds);
      setLicense(initial.license ?? license);
      setBody(initial.body?.blocks?.[0]?.text ?? "");
      setSourceTitle(initial.sources?.[0]?.title ?? "");
      setSourceAuthor(initial.sources?.[0]?.author ?? "");
      setSourceUrl(initial.sources?.[0]?.url ?? "");
      setPassingScore(initial.quiz?.passingScore?.toString() ?? "");
      setMaxAttempts(initial.quiz?.maxAttempts?.toString() ?? "");
      setQuestions(initial.quiz?.questions ?? [emptyQuestion()]);
    }
    // Load once when initial content arrives.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initial?.title, initial?.body, initial?.kind]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(false);
    try {
      await onSubmit({
        kind,
        title,
        summary,
        subjectId,
        educationLevelId,
        parentId: parentId || null,
        sortOrder,
        required,
        competencyIds,
        license,
        body: { blocks: body ? [{ type: "paragraph", text: body }] : [] },
        sources: sourceTitle
          ? [{ title: sourceTitle, author: sourceAuthor, url: sourceUrl }]
          : [],
        quiz:
          kind === "QUIZ"
            ? {
                passingScore: passingScore ? Number(passingScore) : null,
                maxAttempts: maxAttempts ? Number(maxAttempts) : null,
                required,
                questions,
              }
            : undefined,
      });
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  const eligible = competencies.filter((item) =>
    verifiedCompetencyIds.includes(item.id),
  );

  return (
    <form className="stack editor" onSubmit={(event) => void submit(event)}>
      {error ? <p className="error">{copy.saveError}</p> : null}
      <label>
        {copy.contentKind}
        <select value={kind} onChange={(event) => setKind(event.target.value)}>
          {KINDS.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
      </label>
      <label>
        {copy.title}
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          required
        />
      </label>
      <label>
        {copy.summary}
        <textarea
          value={summary}
          onChange={(event) => setSummary(event.target.value)}
        />
      </label>
      <label>
        {copy.subject}
        <select
          value={subjectId}
          onChange={(event) => setSubjectId(event.target.value)}
        >
          {subjects.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        {copy.educationLevel}
        <select
          value={educationLevelId}
          onChange={(event) => setEducationLevelId(event.target.value)}
        >
          {levels.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        {copy.parentId}
        <input
          value={parentId}
          onChange={(event) => setParentId(event.target.value)}
          placeholder="UUID"
        />
      </label>
      <label>
        {copy.sortOrder}
        <input
          type="number"
          min={0}
          value={sortOrder}
          onChange={(event) => setSortOrder(Number(event.target.value))}
        />
      </label>
      <label className="inline-check">
        <input
          type="checkbox"
          checked={required}
          onChange={(event) => setRequired(event.target.checked)}
        />
        {copy.requiredItem}
      </label>
      <label>
        {copy.competency}
        <select
          value={competencyIds[0] ?? ""}
          onChange={(event) =>
            setCompetencyIds(event.target.value ? [event.target.value] : [])
          }
        >
          {eligible.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      {kind !== "QUIZ" ? (
        <label>
          {copy.body}
          <textarea
            rows={10}
            value={body}
            onChange={(event) => setBody(event.target.value)}
            required
          />
        </label>
      ) : (
        <fieldset className="quiz-fieldset">
          <legend>{copy.quizBuilder}</legend>
          <label>
            {copy.passingScore}
            <input
              type="number"
              min={0}
              max={100}
              value={passingScore}
              onChange={(event) => setPassingScore(event.target.value)}
            />
          </label>
          <label>
            {copy.maxAttemptsLabel}
            <input
              type="number"
              min={1}
              value={maxAttempts}
              onChange={(event) => setMaxAttempts(event.target.value)}
            />
          </label>
          {questions.map((question, questionIndex) => (
            <fieldset key={questionIndex} className="quiz-fieldset">
              <legend>
                {copy.question} {questionIndex + 1}
              </legend>
              <label>
                {copy.questionType}
                <select
                  value={question.type}
                  onChange={(event) =>
                    updateQuestion(questionIndex, { type: event.target.value })
                  }
                >
                  <option value="SINGLE_CHOICE">Pilihan tunggal</option>
                  <option value="MULTIPLE_CHOICE">Pilihan ganda</option>
                  <option value="TRUE_FALSE">Benar / salah</option>
                </select>
              </label>
              <label>
                {copy.questionPrompt}
                <textarea
                  required
                  value={question.prompt}
                  onChange={(event) =>
                    updateQuestion(questionIndex, { prompt: event.target.value })
                  }
                />
              </label>
              <label>
                {copy.explanation}
                <textarea
                  value={question.explanation}
                  onChange={(event) =>
                    updateQuestion(questionIndex, { explanation: event.target.value })
                  }
                />
              </label>
              <label>
                {copy.difficulty}
                <select
                  value={question.difficulty}
                  onChange={(event) =>
                    updateQuestion(questionIndex, { difficulty: event.target.value })
                  }
                >
                  <option value="EASY">EASY</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HARD">HARD</option>
                </select>
              </label>
              {question.options.map((option, optionIndex) => (
                <label key={optionIndex} className="quiz-option">
                  <span>{copy.optionText}</span>
                  <input
                    required
                    value={option.text}
                    onChange={(event) =>
                      updateOption(questionIndex, optionIndex, {
                        text: event.target.value,
                      })
                    }
                  />
                  <label className="inline-check">
                    <input
                      type={question.type === "MULTIPLE_CHOICE" ? "checkbox" : "radio"}
                      name={`correct-${questionIndex}`}
                      checked={option.correct}
                      onChange={() =>
                        markCorrect(questionIndex, optionIndex, question.type)
                      }
                    />
                    {copy.optionCorrect}
                  </label>
                </label>
              ))}
            </fieldset>
          ))}
          <button
            type="button"
            className="secondary"
            onClick={() => setQuestions([...questions, emptyQuestion()])}
          >
            {copy.addQuestion}
          </button>
        </fieldset>
      )}
      <label>
        {copy.license}
        <select value={license} onChange={(event) => setLicense(event.target.value)}>
          {licenses.map((item) => (
            <option key={item.code} value={item.code}>
              {item.name}
            </option>
          ))}
        </select>
      </label>
      <p className="muted">{copy.licenseNotice}</p>
      <label>
        {copy.sourceTitle}
        <input
          value={sourceTitle}
          onChange={(event) => setSourceTitle(event.target.value)}
        />
      </label>
      <label>
        {copy.sourceAuthor}
        <input
          value={sourceAuthor}
          onChange={(event) => setSourceAuthor(event.target.value)}
        />
      </label>
      <label>
        {copy.sourceUrl}
        <input
          value={sourceUrl}
          onChange={(event) => setSourceUrl(event.target.value)}
        />
      </label>
      <button type="submit" disabled={busy || competencyIds.length === 0}>
        {submitLabel}
      </button>
    </form>
  );

  function updateQuestion(index: number, patch: Partial<QuizQuestionDraft>) {
    setQuestions((current) =>
      current.map((question, questionIndex) =>
        questionIndex === index ? { ...question, ...patch } : question,
      ),
    );
  }

  function updateOption(
    questionIndex: number,
    optionIndex: number,
    patch: Partial<QuizQuestionDraft["options"][number]>,
  ) {
    setQuestions((current) =>
      current.map((question, qIndex) =>
        qIndex === questionIndex
          ? {
              ...question,
              options: question.options.map((option, oIndex) =>
                oIndex === optionIndex ? { ...option, ...patch } : option,
              ),
            }
          : question,
      ),
    );
  }

  function markCorrect(questionIndex: number, optionIndex: number, type: string) {
    setQuestions((current) =>
      current.map((question, qIndex) => {
        if (qIndex !== questionIndex) {
          return question;
        }
        return {
          ...question,
          options: question.options.map((option, oIndex) => {
            if (type === "MULTIPLE_CHOICE") {
              return oIndex === optionIndex
                ? { ...option, correct: !option.correct }
                : option;
            }
            return { ...option, correct: oIndex === optionIndex };
          }),
        };
      }),
    );
  }
}
