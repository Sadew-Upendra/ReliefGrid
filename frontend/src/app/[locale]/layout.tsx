import type { Metadata, Viewport } from "next";
import { Bricolage_Grotesque, Instrument_Sans } from "next/font/google";
import { NextIntlClientProvider, hasLocale } from "next-intl";
import { getMessages } from "next-intl/server";
import { notFound } from "next/navigation";
import { routing } from "@/i18n/routing";
import ServiceWorkerRegister from "@/components/ServiceWorkerRegister";
import { AuthProvider } from "@/context/AuthContext";
import "./globals.css";

const display = Bricolage_Grotesque({ subsets: ["latin"], variable: "--font-display" });
const body = Instrument_Sans({ subsets: ["latin"], variable: "--font-body" });

export const metadata: Metadata = {
  title: "ReliefGrid",
  description: "Coordinate disaster relief on one live map.",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, statusBarStyle: "default", title: "ReliefGrid" },
};

export const viewport: Viewport = { themeColor: "#0E7C7B" };

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) notFound();
  const messages = await getMessages();

  return (
    <html lang={locale} className={`${display.variable} ${body.variable}`}>
      <body>
        <NextIntlClientProvider locale={locale} messages={messages}>
          <AuthProvider>
            {children}
            <ServiceWorkerRegister />
          </AuthProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
