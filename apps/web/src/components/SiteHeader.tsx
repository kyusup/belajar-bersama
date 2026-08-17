"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { fetchCurrentUser, logout } from "@/lib/api/auth";
import { copy } from "@/lib/i18n/id";

export function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void fetchCurrentUser()
      .then((user) => {
        if (!cancelled) {
          setDisplayName(user?.displayName ?? null);
          setPermissions(user?.permissions ?? []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDisplayName(null);
          setPermissions([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [pathname]);

  async function onLogout() {
    await logout();
    setDisplayName(null);
    setPermissions([]);
    router.push("/");
    router.refresh();
  }

  return (
    <header className="site-header">
      <Link href="/" className="brand">
        {copy.productName}
      </Link>
      <nav>
        <Link href="/">{copy.home}</Link>
        <Link href="/subjek">{copy.subjects}</Link>
        <Link href="/tanya">{copy.qaTitle}</Link>
        {ready && displayName ? <Link href="/belajar">{copy.learn}</Link> : null}
        {ready &&
        (permissions.includes("CONTENT_MODERATE") ||
          permissions.includes("CONTENT_REPORT_REVIEW")) ? (
          <Link href="/moderasi">{copy.moderationTitle}</Link>
        ) : null}
        {ready &&
        (permissions.includes("VERIFICATION_REVIEW") ||
          permissions.includes("ROLE_MANAGE") ||
          permissions.includes("TAXONOMY_MANAGE") ||
          permissions.includes("USER_MANAGE")) ? (
          <Link href="/kelola">{copy.adminTitle}</Link>
        ) : null}
        {ready && permissions.includes("CONTENT_CREATE") ? (
          <Link href="/konten-saya">{copy.myContent}</Link>
        ) : null}
        {ready && permissions.includes("CONTENT_REVIEW") ? (
          <Link href="/tinjauan">{copy.reviews}</Link>
        ) : null}
        <Link href="/status">{copy.statusTitle}</Link>
        {ready && displayName ? (
          <>
            <Link href="/akun">{copy.account}</Link>
            <button type="button" className="linkish" onClick={() => void onLogout()}>
              {copy.logout}
            </button>
          </>
        ) : (
          <Link href="/masuk">{copy.login}</Link>
        )}
      </nav>
    </header>
  );
}
