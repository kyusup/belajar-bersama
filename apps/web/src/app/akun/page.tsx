import { copy } from "@/lib/i18n/id";
import { AccountPanel } from "@/components/AccountPanel";
import { VerificationApply } from "@/components/VerificationApply";

export default function AccountPage() {
  return (
    <article>
      <h1>{copy.accountTitle}</h1>
      <p className="muted">{copy.accountIntro}</p>
      <AccountPanel />
      <VerificationApply />
    </article>
  );
}
