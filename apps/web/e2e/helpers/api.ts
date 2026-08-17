const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";

const MATH = "aaaaaaaa-0001-4000-8000-000000000001";
const SUBJECT_MATH = "bbbbbbbb-0001-4000-8000-000000000001";
const LEVEL_SMP = "cccccccc-0002-4000-8000-000000000001";

export type E2EFixture = {
  materialSlug: string;
  materialTitle: string;
  marker: string;
};

type DevLoginInput = {
  provider: "GOOGLE" | "APPLE";
  subject: string;
  displayName: string;
};

function parseSetCookie(header: string | null): string | undefined {
  if (!header) {
    return undefined;
  }
  const match = header.match(/bb_session=([^;]+)/);
  return match?.[1];
}

export async function waitForHealth(timeoutMs = 120_000): Promise<void> {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(`${API_URL}/api/v1/health`);
      if (response.ok) {
        return;
      }
    } catch {
      // retry
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000));
  }
  throw new Error(`API health check failed at ${API_URL}`);
}

export async function devLogin(input: DevLoginInput): Promise<string> {
  const response = await fetch(`${API_URL}/api/v1/auth/dev/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Origin: "http://localhost:3000" },
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(`Dev login failed (${response.status})`);
  }
  const token = parseSetCookie(response.headers.get("set-cookie"));
  if (!token) {
    throw new Error("Missing bb_session cookie from dev login");
  }
  return token;
}

async function withSession<T>(
  token: string,
  run: (headers: HeadersInit) => Promise<T>,
): Promise<T> {
  return run({
    Cookie: `bb_session=${token}`,
    Origin: "http://localhost:3000",
    "Content-Type": "application/json",
  });
}

async function approveVerification(
  adminToken: string,
  learnerToken: string,
  competencyId: string,
) {
  const create = await withSession(learnerToken, (headers) =>
    fetch(`${API_URL}/api/v1/verifications`, {
      method: "POST",
      headers,
      body: JSON.stringify({ competencyId, qualification: "e2e fixture" }),
    }),
  );
  if (!create.ok) {
    throw new Error(`Verification apply failed (${create.status})`);
  }
  const verification = (await create.json()) as { id: string };
  const approve = await withSession(adminToken, (headers) =>
    fetch(`${API_URL}/api/v1/admin/verifications/${verification.id}/approve`, {
      method: "POST",
      headers,
      body: JSON.stringify({ note: "e2e" }),
    }),
  );
  if (!approve.ok) {
    throw new Error(`Verification approve failed (${approve.status})`);
  }
}

async function assignChecker(adminToken: string, userToken: string) {
  const me = await withSession(userToken, (headers) =>
    fetch(`${API_URL}/api/v1/me`, {
      headers: { Cookie: (headers as Record<string, string>).Cookie },
    }),
  );
  if (!me.ok) {
    throw new Error(`Fetch /me failed (${me.status})`);
  }
  const user = (await me.json()) as { id: string };
  const assign = await withSession(adminToken, (headers) =>
    fetch(`${API_URL}/api/v1/admin/users/${user.id}/roles`, {
      method: "POST",
      headers,
      body: JSON.stringify({ role: "CHECKER" }),
    }),
  );
  if (!assign.ok && assign.status !== 204) {
    throw new Error(`Assign checker failed (${assign.status})`);
  }
}

export async function ensurePublishedMaterialFixture(
  marker: string,
): Promise<E2EFixture> {
  const adminToken = await devLogin({
    provider: "GOOGLE",
    subject: "admin-1",
    displayName: "Admin E2E",
  });
  const makerSubject = `e2e-maker-${marker}`;
  const checkerSubject = `e2e-checker-${marker}`;
  const makerToken = await devLogin({
    provider: "GOOGLE",
    subject: makerSubject,
    displayName: "Maker E2E",
  });
  const checkerToken = await devLogin({
    provider: "GOOGLE",
    subject: checkerSubject,
    displayName: "Checker E2E",
  });

  await approveVerification(adminToken, makerToken, MATH);
  await approveVerification(adminToken, checkerToken, MATH);
  await assignChecker(adminToken, checkerToken);

  const title = `E2E Fixture ${marker}`;
  const draft = {
    kind: "MATERIAL",
    title,
    summary: "Materi uji end-to-end",
    subjectId: SUBJECT_MATH,
    educationLevelId: LEVEL_SMP,
    competencyIds: [MATH],
    license: "CC_BY_SA",
    body: {
      blocks: [{ type: "paragraph", text: `Paragraf ${marker} untuk uji E2E.` }],
    },
    sources: [
      {
        title: "Sumber uji",
        author: "Penulis",
        publisher: "Penerbit",
        url: "https://example.test/e2e",
      },
    ],
  };

  const create = await withSession(makerToken, (headers) =>
    fetch(`${API_URL}/api/v1/content`, {
      method: "POST",
      headers,
      body: JSON.stringify(draft),
    }),
  );
  if (!create.ok) {
    throw new Error(`Create content failed (${create.status})`);
  }
  const created = (await create.json()) as { id: string; slug: string };
  const contentId = created.id;

  const submit = await withSession(makerToken, (headers) =>
    fetch(`${API_URL}/api/v1/content/${contentId}/submit`, { method: "POST", headers }),
  );
  if (!submit.ok) {
    throw new Error(`Submit content failed (${submit.status})`);
  }

  const reviews = await withSession(checkerToken, (headers) =>
    fetch(`${API_URL}/api/v1/reviews/my`, {
      headers: { Cookie: (headers as Record<string, string>).Cookie },
    }),
  );
  if (!reviews.ok) {
    throw new Error(`Review queue failed (${reviews.status})`);
  }
  const queue = (await reviews.json()) as Array<{ id: string; contentId: string }>;
  const submission = queue.find((item) => item.contentId === contentId);
  if (!submission) {
    throw new Error("Submission not found in checker queue");
  }

  const start = await withSession(checkerToken, (headers) =>
    fetch(`${API_URL}/api/v1/reviews/${submission.id}/start`, {
      method: "POST",
      headers,
    }),
  );
  if (!start.ok) {
    throw new Error(`Start review failed (${start.status})`);
  }

  const approve = await withSession(checkerToken, (headers) =>
    fetch(`${API_URL}/api/v1/reviews/${submission.id}/approve`, {
      method: "POST",
      headers,
      body: JSON.stringify({ note: "Layak terbit E2E." }),
    }),
  );
  if (!approve.ok) {
    throw new Error(`Approve review failed (${approve.status})`);
  }

  const publish = await withSession(makerToken, (headers) =>
    fetch(`${API_URL}/api/v1/content/${contentId}/publish`, {
      method: "POST",
      headers,
    }),
  );
  if (!publish.ok) {
    throw new Error(`Publish content failed (${publish.status})`);
  }

  const detail = await withSession(makerToken, (headers) =>
    fetch(`${API_URL}/api/v1/content/${contentId}`, {
      headers: { Cookie: (headers as Record<string, string>).Cookie },
    }),
  );
  if (!detail.ok) {
    throw new Error(`Fetch content failed (${detail.status})`);
  }
  const published = (await detail.json()) as { slug: string; title: string };

  return { materialSlug: published.slug, materialTitle: published.title, marker };
}

export { API_URL };
