import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SiteHeader } from "@/components/SiteHeader";
import { copy } from "@/lib/i18n/id";
import "./globals.css";

export const metadata: Metadata = {
  title: copy.productName,
  description: copy.tagline,
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="id">
      <body>
        <SiteHeader />
        <main>{children}</main>
      </body>
    </html>
  );
}
