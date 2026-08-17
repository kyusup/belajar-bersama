import { expect, $, browser } from "@wdio/globals";

export function webBaseUrl(): string {
  return (globalThis as { bbWebBaseUrl?: string }).bbWebBaseUrl ?? "http://localhost:3000";
}

export async function waitForHeading(text: string) {
  const heading = await $("h1");
  await heading.waitForDisplayed({ timeout: 20_000 });
  await expect(heading).toHaveText(expect.stringContaining(text));
  return heading;
}

export { browser, $, expect };
