import type { Config } from "tailwindcss";
export default {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: { ink: "#0F2A2E", paper: "#F5F7F6", line: "#DCE3E0", brand: "#0E7C7B", "brand-dark": "#0A5F5E" },
      fontFamily: { display: ["var(--font-display)", "sans-serif"], sans: ["var(--font-body)", "system-ui", "sans-serif"] },
    },
  },
  plugins: [],
} satisfies Config;
