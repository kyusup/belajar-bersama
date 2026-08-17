"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { fetchCurrentUser, type CurrentUser } from "@/lib/api/auth";
import {
  acceptAnswer,
  answerQuestion,
  closeQuestion,
  fetchPublicQuestion,
  hideAnswer,
  hideQuestion,
  markUseful,
  reportAnswer,
  reportQuestion,
  unacceptAnswer,
  unmarkUseful,
  type QaQuestion,
} from "@/lib/api/qa";
import { copy } from "@/lib/i18n/id";

export default function TanyaDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [question, setQuestion] = useState<QaQuestion | null>(null);
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [answer, setAnswer] = useState("");
  const [reason, setReason] = useState("INCORRECT");
  const [description, setDescription] = useState("");
  const [reported, setReported] = useState(false);
  const [error, setError] = useState(false);
  const [missing, setMissing] = useState(false);

  async function reload() {
    const next = await fetchPublicQuestion(params.id);
    setQuestion(next);
  }

  useEffect(() => {
    void Promise.all([fetchPublicQuestion(params.id), fetchCurrentUser()])
      .then(([next, nextUser]) => {
        setQuestion(next);
        setUser(nextUser);
      })
      .catch((caught) => {
        if (
          caught &&
          typeof caught === "object" &&
          "status" in caught &&
          caught.status === 404
        ) {
          setMissing(true);
        } else {
          setError(true);
        }
      });
  }, [params.id]);

  if (missing) {
    return <p className="muted">{copy.qaNotFound}</p>;
  }
  if (!question) {
    return <p className="muted">{copy.loading}</p>;
  }

  const thread = question;
  const canModerate = Boolean(user?.permissions.includes("CONTENT_MODERATE"));
  const isAsker = user?.id === thread.authorId;

  async function onAnswer(event: FormEvent) {
    event.preventDefault();
    try {
      setQuestion(await answerQuestion(thread.id, answer));
      setAnswer("");
    } catch {
      setError(true);
    }
  }

  async function onReport(event: FormEvent) {
    event.preventDefault();
    try {
      await reportQuestion(thread.id, reason, description);
      setReported(true);
    } catch {
      setError(true);
    }
  }

  return (
    <article>
      <p>
        <Link href={thread.contentId ? `/tanya?content=${thread.contentId}` : "/tanya"}>
          {copy.qaTitle}
        </Link>
      </p>
      <h1>{thread.title}</h1>
      <p className="muted">
        {thread.authorDisplayName} ·{" "}
        {thread.status === "CLOSED" ? copy.qaClosed : copy.qaOpen}
      </p>
      <div className="content-body">
        <p>{thread.body}</p>
      </div>
      {error ? <p className="error">{copy.saveError}</p> : null}
      {(isAsker || canModerate) && thread.status === "OPEN" ? (
        <p>
          <button
            type="button"
            className="secondary"
            onClick={() => void closeQuestion(thread.id).then(setQuestion)}
          >
            {copy.qaClose}
          </button>
        </p>
      ) : null}
      {canModerate ? (
        <p>
          <button
            type="button"
            className="secondary"
            onClick={() =>
              void hideQuestion(thread.id).then(() => {
                router.push("/tanya");
              })
            }
          >
            {copy.qaHide}
          </button>
        </p>
      ) : null}
      <section>
        <h2>{copy.qaAnswers}</h2>
        {thread.answers.length === 0 ? (
          <p className="muted">{copy.qaNoAnswers}</p>
        ) : (
          <ul className="card-list">
            {thread.answers.map((item) => (
              <li
                key={item.id}
                className={item.accepted ? "accepted-answer" : undefined}
              >
                {item.accepted ? <p className="badge">{copy.qaAccepted}</p> : null}
                <p>{item.body}</p>
                <p className="muted">
                  {item.authorDisplayName} · {copy.qaUsefulCount}: {item.usefulCount}
                </p>
                {user ? (
                  <p className="stack">
                    {user.id !== item.authorId ? (
                      <button
                        type="button"
                        className="secondary"
                        onClick={() =>
                          void (
                            item.markedUseful
                              ? unmarkUseful(item.id)
                              : markUseful(item.id)
                          ).then(() => void reload())
                        }
                      >
                        {item.markedUseful ? copy.qaUnmarkUseful : copy.qaMarkUseful}
                      </button>
                    ) : null}
                    {isAsker || canModerate ? (
                      <button
                        type="button"
                        className="secondary"
                        onClick={() =>
                          void (
                            item.accepted
                              ? unacceptAnswer(thread.id)
                              : acceptAnswer(thread.id, item.id)
                          ).then(setQuestion)
                        }
                      >
                        {item.accepted ? copy.qaUnaccept : copy.qaAccept}
                      </button>
                    ) : null}
                    {canModerate ? (
                      <button
                        type="button"
                        className="secondary"
                        onClick={() => void hideAnswer(item.id).then(reload)}
                      >
                        {copy.qaHideAnswer}
                      </button>
                    ) : null}
                    <ReportAnswerButton
                      answerId={item.id}
                      onError={() => setError(true)}
                    />
                  </p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
      {user && thread.status === "OPEN" ? (
        <form className="panel stack editor" onSubmit={(event) => void onAnswer(event)}>
          <h2>{copy.qaAnswer}</h2>
          <label>
            {copy.body}
            <textarea
              required
              value={answer}
              onChange={(event) => setAnswer(event.target.value)}
            />
          </label>
          <button type="submit">{copy.qaAnswer}</button>
        </form>
      ) : null}
      {!user ? (
        <p>
          {copy.qaNeedLogin} <Link href="/masuk">{copy.login}</Link>
        </p>
      ) : null}
      <section className="panel">
        <h2>{copy.report}</h2>
        {!user ? (
          <p>
            {copy.reportNeedLogin} <Link href="/masuk">{copy.login}</Link>
          </p>
        ) : reported ? (
          <p>{copy.reportThanks}</p>
        ) : (
          <form className="stack" onSubmit={(event) => void onReport(event)}>
            <label>
              {copy.reportReason}
              <select
                value={reason}
                onChange={(event) => setReason(event.target.value)}
              >
                <option value="INCORRECT">Informasi tidak tepat</option>
                <option value="COPYRIGHT">Hak cipta</option>
                <option value="INAPPROPRIATE">Tidak pantas</option>
                <option value="SPAM">Spam</option>
                <option value="OTHER">Lainnya</option>
              </select>
            </label>
            <label>
              {copy.reportDescription}
              <textarea
                required
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
            <button type="submit">{copy.reportSubmit}</button>
          </form>
        )}
      </section>
    </article>
  );
}

function ReportAnswerButton({
  answerId,
  onError,
}: {
  answerId: string;
  onError: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("INAPPROPRIATE");
  const [description, setDescription] = useState("");
  const [done, setDone] = useState(false);

  if (done) {
    return <p>{copy.reportThanks}</p>;
  }
  if (!open) {
    return (
      <button type="button" className="secondary" onClick={() => setOpen(true)}>
        {copy.report}
      </button>
    );
  }
  return (
    <form
      className="stack editor"
      onSubmit={(event) => {
        event.preventDefault();
        void reportAnswer(answerId, reason, description)
          .then(() => setDone(true))
          .catch(onError);
      }}
    >
      <label>
        {copy.reportReason}
        <select value={reason} onChange={(event) => setReason(event.target.value)}>
          <option value="INCORRECT">Informasi tidak tepat</option>
          <option value="COPYRIGHT">Hak cipta</option>
          <option value="INAPPROPRIATE">Tidak pantas</option>
          <option value="SPAM">Spam</option>
          <option value="OTHER">Lainnya</option>
        </select>
      </label>
      <label>
        {copy.reportDescription}
        <textarea
          required
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </label>
      <button type="submit">{copy.reportSubmit}</button>
    </form>
  );
}
