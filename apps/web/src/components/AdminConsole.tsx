"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import {
  fetchCompetencies,
  fetchCurrentUser,
  type Competency,
  type CurrentUser,
} from "@/lib/api/auth";
import {
  assignRole,
  createEducationLevel,
  createSubject,
  decideVerification,
  fetchAdminUsers,
  fetchPendingVerifications,
  reactivateUser,
  revokeRole,
  suspendUser,
  type AdminUser,
  type Verification,
} from "@/lib/api/admin";
import { copy } from "@/lib/i18n/id";

export function AdminConsole() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(undefined);
  const [pending, setPending] = useState<Verification[]>([]);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [note, setNote] = useState("Ditinjau.");
  const [roleUserId, setRoleUserId] = useState("");
  const [role, setRole] = useState("CHECKER");
  const [subjectName, setSubjectName] = useState("");
  const [subjectDescription, setSubjectDescription] = useState("");
  const [levelName, setLevelName] = useState("");
  const [userQuery, setUserQuery] = useState("");
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState(false);

  async function reload(nextUser: CurrentUser) {
    if (nextUser.permissions.includes("VERIFICATION_REVIEW")) {
      setPending(await fetchPendingVerifications());
    }
    if (nextUser.permissions.includes("USER_MANAGE")) {
      const page = await fetchAdminUsers();
      setAdminUsers(page.items);
    }
    setCompetencies(await fetchCompetencies());
  }

  useEffect(() => {
    void fetchCurrentUser()
      .then(async (nextUser) => {
        setUser(nextUser);
        if (nextUser) {
          await reload(nextUser);
        }
      })
      .catch(() => setError(true));
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
  const canAdmin =
    user.permissions.includes("VERIFICATION_REVIEW") ||
    user.permissions.includes("ROLE_MANAGE") ||
    user.permissions.includes("TAXONOMY_MANAGE") ||
    user.permissions.includes("USER_MANAGE");
  if (!canAdmin) {
    return <p>{copy.adminDenied}</p>;
  }

  async function onDecide(
    id: string,
    action: "approve" | "reject" | "request-changes",
  ) {
    try {
      await decideVerification(id, action, note);
      setPending(await fetchPendingVerifications());
    } catch {
      setError(true);
    }
  }

  async function onRole(event: FormEvent, revoke: boolean) {
    event.preventDefault();
    try {
      if (revoke) {
        await revokeRole(roleUserId, role);
      } else {
        await assignRole(roleUserId, role);
      }
    } catch {
      setError(true);
    }
  }

  async function onSubject(event: FormEvent) {
    event.preventDefault();
    try {
      await createSubject(subjectName, subjectDescription);
      setSubjectName("");
      setSubjectDescription("");
    } catch {
      setError(true);
    }
  }

  async function onLevel(event: FormEvent) {
    event.preventDefault();
    try {
      await createEducationLevel(levelName, 100);
      setLevelName("");
    } catch {
      setError(true);
    }
  }

  return (
    <article>
      <h1>{copy.adminTitle}</h1>
      <p className="muted">{copy.adminIntro}</p>
      {error ? <p className="error">{copy.saveError}</p> : null}
      {user.permissions.includes("USER_MANAGE") ? (
        <section>
          <h2>{copy.manageUsers}</h2>
          <p className="muted">{copy.manageUsersIntro}</p>
          <form
            className="search-bar"
            onSubmit={(event) => {
              event.preventDefault();
              void fetchAdminUsers(userQuery)
                .then((page) => setAdminUsers(page.items))
                .catch(() => setError(true));
            }}
          >
            <label className="editor">
              {copy.searchUsers}
              <input
                value={userQuery}
                onChange={(event) => setUserQuery(event.target.value)}
                aria-label={copy.searchUsers}
              />
            </label>
            <button type="submit">{copy.searchAction}</button>
          </form>
          {adminUsers.length === 0 ? (
            <p className="muted">{copy.noUsers}</p>
          ) : (
            <ul className="card-list">
              {adminUsers.map((item) => (
                <li key={item.id}>
                  <p>
                    <button
                      type="button"
                      className="linkish"
                      onClick={() => setRoleUserId(item.id)}
                    >
                      {item.displayName}
                    </button>
                  </p>
                  <p className="muted">
                    {item.status} · {item.storedRoles.join(", ")}
                  </p>
                  <p className="stack">
                    {item.status === "ACTIVE" ? (
                      <button
                        type="button"
                        className="secondary"
                        onClick={() =>
                          void suspendUser(item.id)
                            .then(() => fetchAdminUsers(userQuery))
                            .then((page) => setAdminUsers(page.items))
                            .catch(() => setError(true))
                        }
                      >
                        {copy.suspendUser}
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="secondary"
                        onClick={() =>
                          void reactivateUser(item.id)
                            .then(() => fetchAdminUsers(userQuery))
                            .then((page) => setAdminUsers(page.items))
                            .catch(() => setError(true))
                        }
                      >
                        {copy.reactivateUser}
                      </button>
                    )}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
      {user.permissions.includes("VERIFICATION_REVIEW") ? (
        <section>
          <h2>{copy.pendingVerifications}</h2>
          <label>
            {copy.reviewComment}
            <input value={note} onChange={(event) => setNote(event.target.value)} />
          </label>
          {pending.length === 0 ? (
            <p className="muted">{copy.emptyVerifications}</p>
          ) : (
            <ul className="card-list">
              {pending.map((item) => (
                <li key={item.id}>
                  <p>
                    {competencies.find(
                      (competency) => competency.id === item.competencyId,
                    )?.name || item.competencyId}
                  </p>
                  <p className="muted">
                    {copy.applicantId}: {item.applicantId}
                  </p>
                  <p>{item.qualification}</p>
                  <p className="stack">
                    <button
                      type="button"
                      onClick={() => void onDecide(item.id, "approve")}
                    >
                      {copy.approve}
                    </button>
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => void onDecide(item.id, "request-changes")}
                    >
                      {copy.requestChanges}
                    </button>
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => void onDecide(item.id, "reject")}
                    >
                      {copy.rejectVerification}
                    </button>
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
      {user.permissions.includes("ROLE_MANAGE") ? (
        <form
          className="panel stack editor"
          onSubmit={(event) => void onRole(event, false)}
        >
          <h2>{copy.manageRoles}</h2>
          <p className="muted">{copy.manageRolesIntro}</p>
          <label>
            {copy.userId}
            <input
              required
              value={roleUserId}
              onChange={(event) => setRoleUserId(event.target.value)}
            />
          </label>
          <label>
            {copy.roles}
            <select value={role} onChange={(event) => setRole(event.target.value)}>
              <option value="CHECKER">CHECKER</option>
              <option value="MODERATOR">MODERATOR</option>
              <option value="ADMINISTRATOR">ADMINISTRATOR</option>
            </select>
          </label>
          <button type="submit">{copy.assignRole}</button>
          <button
            type="button"
            className="secondary"
            onClick={(event) => void onRole(event, true)}
          >
            {copy.revokeRole}
          </button>
        </form>
      ) : null}
      {user.permissions.includes("TAXONOMY_MANAGE") ? (
        <>
          <form
            className="panel stack editor"
            onSubmit={(event) => void onSubject(event)}
          >
            <h2>{copy.createSubject}</h2>
            <label>
              {copy.title}
              <input
                required
                value={subjectName}
                onChange={(event) => setSubjectName(event.target.value)}
              />
            </label>
            <label>
              {copy.summary}
              <textarea
                value={subjectDescription}
                onChange={(event) => setSubjectDescription(event.target.value)}
              />
            </label>
            <button type="submit">{copy.createSubject}</button>
          </form>
          <form
            className="panel stack editor"
            onSubmit={(event) => void onLevel(event)}
          >
            <h2>{copy.createLevel}</h2>
            <label>
              {copy.title}
              <input
                required
                value={levelName}
                onChange={(event) => setLevelName(event.target.value)}
              />
            </label>
            <button type="submit">{copy.createLevel}</button>
          </form>
        </>
      ) : null}
    </article>
  );
}
