import Image from "next/image";
import { Link } from "@/i18n/navigation";

// logo-horizontal.png is 682x144
const RATIO = 682 / 144;

export default function Logo({ height = 40, priority = false }: { height?: number; priority?: boolean }) {
  return (
    <Link href="/" aria-label="ReliefGrid home" className="inline-flex items-center">
      <Image
        src="/brand/logo-horizontal.png"
        alt="ReliefGrid"
        width={Math.round(height * RATIO)}
        height={height}
        priority={priority}
      />
    </Link>
  );
}