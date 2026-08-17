import { copy } from "@/lib/i18n/id";
import { LoginPanel } from "@/components/LoginPanel";
import { Suspense } from "react";

export default function LoginPage() {
  return (
    <article>
      <h1>{copy.loginTitle}</h1>
      <p>{copy.loginIntro}</p>
      <p className="muted">{copy.anonymousNote}</p>
      <Suspense fallback={<p className="muted">{copy.loading}</p>}>
        <LoginPanel />
      </Suspense>
    </article>
  );
}
