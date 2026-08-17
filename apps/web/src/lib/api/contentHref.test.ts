import { describe, expect, it } from "vitest";
import { contentHref, searchHref } from "@/lib/api/content";

describe("searchHref", () => {
  it("routes published Q&A hits to the question page", () => {
    expect(searchHref("QA_QUESTION", "abc")).toBe("/tanya/abc");
    expect(contentHref("COURSE", "dasar")).toBe("/kursus/dasar");
  });
});
