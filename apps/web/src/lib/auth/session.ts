export type AuthState =
  { status: "anonymous" } | { status: "authenticated"; displayName: string };

/**
 * Client-side convenience state only. The API session cookie is the authority.
 */
export function toAuthState(displayName: string | null | undefined): AuthState {
  if (!displayName) {
    return { status: "anonymous" };
  }
  return { status: "authenticated", displayName };
}
