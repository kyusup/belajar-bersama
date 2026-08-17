import { test, expect } from "@playwright/test";
import { copy } from "../src/lib/i18n/id";

test.describe("Smoke", () => {
  test("home page loads and shows product identity", async ({ page }) => {
    await page.goto("/");
    await expect(
      page.getByRole("heading", { level: 1, name: copy.productName }),
    ).toBeVisible();
    await expect(page.getByText(copy.tagline)).toBeVisible();
    await expect(page.getByRole("link", { name: copy.browseSubjects })).toBeVisible();
  });

  test("status page reports API connectivity", async ({ page }) => {
    await page.goto("/status");
    await expect(page.getByRole("heading", { name: copy.statusTitle })).toBeVisible();
    await expect(page.getByText(copy.apiReachable)).toBeVisible({ timeout: 20_000 });
  });

  test("subjects page lists seeded taxonomy", async ({ page }) => {
    await page.goto("/subjek");
    await expect(page.getByRole("heading", { name: copy.subjects })).toBeVisible();
    await expect(page.getByRole("link", { name: /Matematika/i })).toBeVisible();
  });
});
