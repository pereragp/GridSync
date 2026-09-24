const LOGO_SRC = "/GridSync_Logo_bgremoved.png";

/**
 * System brand mark. The asset already includes the GridSync wordmark.
 * Use `variant` to size for header, footer, or hero placements.
 */
export default function BrandLogo({
  variant = "header",
  className = "",
  alt = "GridSync",
}) {
  const sizes = {
    header: "h-16 sm:h-20",
    footer: "h-20",
    hero: "h-36 sm:h-44",
    auth: "h-20 sm:h-24",
  };

  return (
    <img
      src={LOGO_SRC}
      alt={alt}
      className={`w-auto object-contain object-left ${sizes[variant] ?? sizes.header} ${className}`}
      decoding="async"
    />
  );
}
