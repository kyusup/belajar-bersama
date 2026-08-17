const host = process.env.APPIUM_HOST ?? "localhost";
const port = process.env.APPIUM_PORT ?? "4725";

const response = await fetch(`http://${host}:${port}/status`);
if (!response.ok) {
  console.error(`Appium /status failed: HTTP ${response.status}`);
  process.exit(1);
}

const body = await response.json();
if (!body?.value?.ready) {
  console.error("Appium not ready:", JSON.stringify(body));
  process.exit(1);
}

console.log(`Appium ready at http://${host}:${port}`);
