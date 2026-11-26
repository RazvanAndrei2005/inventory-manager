import type { ApiErrorResponse, JwtResponse } from "@/types/api";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/+$/, "") ||
  "https://inventory-manager-cyfw.onrender.com";

const ACCESS_TOKEN_KEY = "im_access_token";
const REFRESH_TOKEN_KEY = "im_refresh_token";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function getStoredTokens(): { accessToken: string | null; refreshToken: string | null } {
  if (typeof window === "undefined") {
    return { accessToken: null, refreshToken: null };
  }
  return {
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
  };
}

export function setStoredTokens(accessToken: string, refreshToken: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearStoredTokens(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

let refreshPromise: Promise<JwtResponse | null> | null = null;

async function refreshAccessToken(): Promise<JwtResponse | null> {
  const { refreshToken } = getStoredTokens();
  if (!refreshToken) return null;

  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) {
          clearStoredTokens();
          return null;
        }
        const data = (await res.json()) as JwtResponse;
        setStoredTokens(data.accessToken, data.refreshToken);
        return data;
      } catch {
        clearStoredTokens();
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }

  return refreshPromise;
}

async function parseErrorMessage(res: Response): Promise<string> {
  try {
    const contentType = res.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      const data = (await res.json()) as Partial<ApiErrorResponse>;
      if (data.message) return data.message;
      if (data.error) return data.error;
    } else {
      const text = await res.text();
      if (text) return text;
    }
  } catch {
    // ignore parse errors
  }
  return `Request failed with status ${res.status}`;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { skipAuth?: boolean } = {}
): Promise<T> {
  const { skipAuth = false, headers: customHeaders, ...rest } = options;

  const buildHeaders = (token: string | null): HeadersInit => ({
    ...(rest.body ? { "Content-Type": "application/json" } : {}),
    ...(token && !skipAuth ? { Authorization: `Bearer ${token}` } : {}),
    ...(customHeaders as Record<string, string>),
  });

  const { accessToken } = getStoredTokens();
  let res = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: buildHeaders(accessToken),
  });

  if (res.status === 401 && !skipAuth) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      res = await fetch(`${API_BASE_URL}${path}`, {
        ...rest,
        headers: buildHeaders(refreshed.accessToken),
      });
    }
  }

  if (!res.ok) {
    const msg = await parseErrorMessage(res);
    throw new ApiError(res.status, msg);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return (await res.json()) as T;
  }
  return (await res.text()) as unknown as T;
}
