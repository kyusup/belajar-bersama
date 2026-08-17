import { Suspense } from "react";
import { TanyaBoard } from "@/components/TanyaBoard";
import { copy } from "@/lib/i18n/id";

export default function TanyaPage() {
  return (
    <Suspense fallback={<p className="muted">{copy.loading}</p>}>
      <TanyaBoard />
    </Suspense>
  );
}
