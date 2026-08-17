import type { Options } from "@wdio/types";

const appiumHost = process.env.APPIUM_HOST ?? "localhost";
const appiumPort = Number(process.env.APPIUM_PORT ?? "4725");
const webBaseUrl = process.env.WEB_BASE_URL ?? "http://localhost:3000";
const runMobile = process.env.RUN_MOBILE_TESTS === "true";
const androidUdid = process.env.ANDROID_UDID ?? "emulator-5554";
const chromeBinary = process.env.CHROME_BINARY ?? "/usr/bin/google-chrome";

export const config: Options.Testrunner = {
  runner: "local",
  hostname: appiumHost,
  port: appiumPort,
  path: "/",
  specs: ["./specs/**/*.spec.ts"],
  maxInstances: 1,
  capabilities: runMobile
    ? [
        {
          platformName: "Android",
          "appium:automationName": "UiAutomator2",
          "appium:deviceName": "Android Emulator",
          "appium:udid": androidUdid,
          browserName: "Chrome",
          "appium:chromedriverAutodownload": true,
          "appium:newCommandTimeout": 120,
        },
      ]
    : [
        {
          platformName: "linux",
          browserName: "chrome",
          "appium:automationName": "Chromium",
          "appium:chromedriverAutodownload": true,
          "goog:chromeOptions": {
            binary: chromeBinary,
            args: [
              "--headless=new",
              "--no-sandbox",
              "--disable-gpu",
              "--disable-dev-shm-usage",
              "--window-size=393,851",
            ],
          },
        },
      ],
  logLevel: "info",
  bail: 0,
  waitforTimeout: 20_000,
  connectionRetryTimeout: 120_000,
  connectionRetryCount: 3,
  framework: "mocha",
  reporters: ["spec"],
  mochaOpts: {
    ui: "bdd",
    timeout: 120_000,
  },
  before: async () => {
    await browser.setTimeout({ pageLoad: 30_000, script: 20_000 });
    (globalThis as { bbWebBaseUrl?: string }).bbWebBaseUrl = webBaseUrl;
  },
};
