/**
 * Full-bleed page hero: edge-to-edge image, dark overlay,
 * soft fade into the page background at the bottom.
 * Text stays high under the nav; the image can extend below it.
 */
export default function PageBleedHero({
  image,
  imageAlt = "",
  eyebrow,
  title,
  subtitle,
  actions,
  children,
  size = "default",
}) {
  // Section height = how far the image runs (text position is independent).
  const sectionHeights =
    size === "home"
      ? "min-h-[17rem] sm:min-h-[19rem] lg:min-h-[20rem]"
      : size === "tall"
        ? "min-h-[20rem] sm:min-h-[22rem] lg:min-h-[24rem]"
        : "min-h-[17.5rem] sm:min-h-[19.5rem] lg:min-h-[21rem]";

  const padding =
    size === "home"
      ? "gap-5 px-4 pt-8 sm:px-6 sm:pt-[2.125rem]"
      : "gap-5 px-4 pt-8 sm:px-6 sm:pt-9";

  return (
    <section
      className={`relative left-1/2 w-screen max-w-[100vw] -translate-x-1/2 -mt-8 mb-8 overflow-hidden ${sectionHeights}`}
    >
      <div className="absolute inset-0">
        <img
          src={image}
          alt={imageAlt}
          className="gs-animate-pan h-full w-full object-cover object-[center_40%]"
        />
        <div className="absolute inset-0 bg-gradient-to-br from-grid-900/88 via-grid-800/68 to-grid-700/42" />
        <div className="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-grid-50 via-grid-50/80 to-transparent" />
      </div>

      <div
        className={`relative z-10 mx-auto flex max-w-6xl flex-col justify-start lg:flex-row lg:items-end lg:justify-between ${padding}`}
      >
        <div className="min-w-0">
          {children ? (
            children
          ) : (
            <>
              {eyebrow ? (
                <p className="gs-animate-fade-up text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
                  {eyebrow}
                </p>
              ) : null}
              {title ? (
                <h1 className="gs-animate-fade-up mt-2 font-display text-3xl font-semibold leading-[1.1] tracking-tight text-white sm:mt-2.5 sm:text-4xl lg:text-[2.75rem]">
                  {title}
                </h1>
              ) : null}
              {subtitle ? (
                <p
                  className="gs-animate-fade-up mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base"
                  style={{ animationDelay: "120ms" }}
                >
                  {subtitle}
                </p>
              ) : null}
            </>
          )}
        </div>

        {actions ? (
          <div
            className="gs-animate-fade-up flex flex-wrap gap-3 lg:shrink-0"
            style={{ animationDelay: "220ms" }}
          >
            {actions}
          </div>
        ) : null}
      </div>
    </section>
  );
}
