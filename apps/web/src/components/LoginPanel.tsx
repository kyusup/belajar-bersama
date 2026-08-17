"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  devLogin,
  fetchAuthConfig,
  startProviderLogin,
  type AuthConfig,
} from "@/lib/api/auth";
import { copy } from "@/lib/i18n/id";

export function LoginPanel() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const denied = searchParams.get("error") === "denied";
  const [config, setConfig] = useState<AuthConfig | null>(null);
  const [subject, setSubject] = useState("local-user");
  const [displayName, setDisplayName] = useState("Pengguna lokal");
  const [provider, setProvider] = useState<"GOOGLE" | "APPLE">("GOOGLE");
  const [error, setError] = useState<string | null>(denied ? copy.loginError : null);

  useEffect(() => {
    void fetchAuthConfig()
      .then(setConfig)
      .catch(() => setConfig({ google: false, apple: false, devLogin: false }));
  }, []);

  async function onDevLogin(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await devLogin({ provider, subject, displayName });
      router.push("/akun");
      router.refresh();
    } catch {
      setError(copy.loginError);
    }
  }

  return (
    <div className="stack">
      {error ? <p className="error">{error}</p> : null}
      {config?.google ? (
        <a className="button" href={startProviderLogin("google")}>
          {copy.loginGoogle}
        </a>
      ) : (
        <p className="muted">
          {copy.loginGoogle}: {copy.loginUnavailable}
        </p>
      )}
      {config?.apple ? (
        <a className="button" href={startProviderLogin("apple")}>
          {copy.loginApple}
        </a>
      ) : (
        <p className="muted">
          {copy.loginApple}: {copy.loginUnavailable}
        </p>
      )}
      {config?.devLogin ? (
        <form className="panel stack" onSubmit={(event) => void onDevLogin(event)}>
          <h2>{copy.devLoginTitle}</h2>
          <p className="muted">{copy.devLoginIntro}</p>
          <label>
            {copy.devProvider}
            <select
              value={provider}
              onChange={(event) =>
                setProvider(event.target.value as "GOOGLE" | "APPLE")
              }
            >
              <option value="GOOGLE">Google</option>
              <option value="APPLE">Apple</option>
            </select>
          </label>
          <label>
            {copy.devSubject}
            <input
              value={subject}
              onChange={(event) => setSubject(event.target.value)}
              required
            />
          </label>
          <label>
            {copy.devName}
            <input
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              required
            />
          </label>
          <button type="submit">{copy.submitDevLogin}</button>
        </form>
      ) : null}
    </div>
  );
}
