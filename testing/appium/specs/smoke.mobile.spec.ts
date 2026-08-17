import { expect, browser, $ } from "@wdio/globals";
import { waitForHeading, webBaseUrl } from "../helpers/app.js";

describe("Belajar Bersama mobile web smoke", () => {
  it("shows product home on a mobile viewport", async () => {
    await browser.url(webBaseUrl());
    await waitForHeading("Belajar Bersama");
    await expect($("body")).toHaveText(expect.stringContaining("Platform belajar terbuka"));
  });

  it("status page reports API connectivity", async () => {
    await browser.url(`${webBaseUrl()}/status`);
    await waitForHeading("Status sistem");
    await expect($("body")).toHaveText(expect.stringContaining("API dapat dijangkau"));
  });
});
