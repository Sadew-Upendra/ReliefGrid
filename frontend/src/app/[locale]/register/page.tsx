"use client";
import { FormEvent, useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter, Link } from "@/i18n/navigation";
import { useAuth } from "@/context/AuthContext";
import { Role, SELF_REGISTER_ROLES } from "@/lib/api";

const ROLE_LABEL_KEY: Record<Role, string> = {
  CITIZEN: "roleCitizen", VOLUNTEER: "roleVolunteer", NGO: "roleNgo",
  COORDINATOR: "roleCitizen", ADMIN: "roleCitizen", // never offered — see SELF_REGISTER_ROLES
};

export default function RegisterPage() {
  const t = useTranslations("auth");
  const router = useRouter();
  const { register } = useAuth();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("CITIZEN");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await register({ name, email, password, role });
      router.replace("/dashboard");
    } catch {
      setError(t("registerError"));
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-6">
      <h1 className="font-display text-2xl font-bold">{t("register")}</h1>
      <form onSubmit={submit} className="mt-6 space-y-4">
        <label className="block text-sm font-medium">
          {t("fullName")}
          <input required autoComplete="name" value={name} onChange={(e) => setName(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-line px-3 py-2 font-normal" />
        </label>
        <label className="block text-sm font-medium">
          {t("email")}
          <input type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-line px-3 py-2 font-normal" />
        </label>
        <label className="block text-sm font-medium">
          {t("password")}
          <input type="password" required minLength={8} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)}
            className="mt-1.5 w-full rounded-lg border border-line px-3 py-2 font-normal" />
        </label>
        <fieldset>
          <legend className="mb-2 text-sm font-medium">{t("role")}</legend>
          <div className="flex flex-wrap gap-2">
            {SELF_REGISTER_ROLES.map((r) => (
              <button type="button" key={r} onClick={() => setRole(r)} aria-pressed={role === r}
                className={`rounded-full border px-3.5 py-1.5 text-sm transition ${role === r ? "border-brand bg-brand text-white" : "border-line hover:border-brand"}`}>
                {t(ROLE_LABEL_KEY[r])}
              </button>
            ))}
          </div>
        </fieldset>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button disabled={busy} className="w-full rounded-lg bg-brand py-2.5 font-medium text-white transition hover:bg-brand-dark disabled:opacity-40">
          {busy ? t("registering") : t("submitRegister")}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-ink/60">
        {t("haveAccount")} <Link href="/login" className="font-medium text-brand hover:text-brand-dark">{t("logInLink")}</Link>
      </p>
    </main>
  );
}
