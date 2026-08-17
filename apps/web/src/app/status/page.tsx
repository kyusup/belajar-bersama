import { PlatformStatusPanel } from "@/components/PlatformStatusPanel";
import { copy } from "@/lib/i18n/id";

export default function StatusPage() {
  return (
    <article>
      <h1>{copy.statusTitle}</h1>
      <p className="muted">{copy.statusIntro}</p>
      <PlatformStatusPanel />
    </article>
  );
}
