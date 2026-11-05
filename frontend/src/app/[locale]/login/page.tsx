"use client";
import { FormEvent, Suspense, useState } from "react";
import { useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { useRouter, Link } from "@/i18n/navigation";
import { useAuth } from "@/context/AuthContext";

function LoginForm() {
  const t = useTranslations("auth");
  const router = useRouter();
  const params = useSearchParams();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login({ email, password });
      router.replace(params.get("next") || "/dashboard");
    } catch {
      setError(t("loginError"));
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-6">
      <h1 className="font-display text-2xl font-bold">{t("login")}</h1>
      <form onSubmit={submit} className="mt-6 space-y-4">
        <label className="block text-sm font-medium">
          {t("email")}
          <input type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-line px-3 py-2 font-normal" />
        </label>
        <label className="block text-sm font-medium">
          {t("password")}
          <input type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-line px-3 py-2 font-normal" />
        </label>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button disabled={busy} className="w-full rounded-lg bg-brand py-2.5 font-medium text-white transition hover:bg-brand-dark disabled:opacity-40">
          {busy ? t("loggingIn") : t("submitLogin")}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-ink/60">
        {t("noAccount")} <Link href="/register" className="font-medium text-brand hover:text-brand-dark">{t("signUpLink")}</Link>
      </p>
    </main>
  );
}

export default function LoginPage() {
  return <Suspense><LoginForm /></Suspense>;
}
