const VARIANTS = {
  error: {
    wrap: "border-red-200 bg-red-50 text-red-900",
    icon: "bg-red-100 text-red-700",
    title: "text-red-950",
    dismiss: "text-red-500 hover:bg-red-100 hover:text-red-800",
  },
  success: {
    wrap: "border-grid-200 bg-grid-50 text-grid-900",
    icon: "bg-grid-100 text-grid-700",
    title: "text-grid-950",
    dismiss: "text-grid-500 hover:bg-grid-100 hover:text-grid-800",
  },
  warning: {
    wrap: "border-amber-200 bg-amber-50 text-amber-950",
    icon: "bg-amber-100 text-amber-800",
    title: "text-amber-950",
    dismiss: "text-amber-600 hover:bg-amber-100 hover:text-amber-900",
  },
  info: {
    wrap: "border-slate-200 bg-slate-50 text-slate-800",
    icon: "bg-slate-200 text-slate-700",
    title: "text-slate-900",
    dismiss: "text-slate-500 hover:bg-slate-200 hover:text-slate-800",
  },
};

const ICONS = {
  error: (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-4.25a.75.75 0 011.5 0v.01a.75.75 0 01-1.5 0V13.75zM10 5.5a.75.75 0 00-.75.75v4.5a.75.75 0 001.5 0v-4.5A.75.75 0 0010 5.5z"
        clipRule="evenodd"
      />
    </svg>
  ),
  success: (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.53-9.97a.75.75 0 00-1.06-1.06L9 10.44 7.53 8.97a.75.75 0 10-1.06 1.06l2 2a.75.75 0 001.06 0l4-4z"
        clipRule="evenodd"
      />
    </svg>
  ),
  warning: (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M8.257 3.099c.765-1.36 2.721-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM10 7a.75.75 0 01.75.75v2.5a.75.75 0 01-1.5 0v-2.5A.75.75 0 0110 7zm0 6.25a.875.875 0 100-1.75.875.875 0 000 1.75z"
        clipRule="evenodd"
      />
    </svg>
  ),
  info: (
    <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-4.75a.75.75 0 001.5 0v-4.5a.75.75 0 00-1.5 0v4.5zM10 6.25a.875.875 0 100 1.75.875.875 0 000-1.75z"
        clipRule="evenodd"
      />
    </svg>
  ),
};

/**
 * Inline or toast-style status message.
 * @param {"error"|"success"|"warning"|"info"} type
 */
export default function AlertMessage({
  type = "error",
  title,
  children,
  onDismiss,
  className = "",
  role,
}) {
  const styles = VARIANTS[type] || VARIANTS.info;
  const resolvedRole =
    role || (type === "error" || type === "warning" ? "alert" : "status");

  return (
    <div
      role={resolvedRole}
      className={`gs-alert flex gap-3 rounded-xl border px-4 py-3 shadow-sm ${styles.wrap} ${className}`}
    >
      <span
        className={`mt-0.5 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full ${styles.icon}`}
      >
        {ICONS[type] || ICONS.info}
      </span>

      <div className="min-w-0 flex-1">
        {title ? (
          <p className={`text-sm font-semibold ${styles.title}`}>{title}</p>
        ) : null}
        <div className={`text-sm leading-relaxed ${title ? "mt-0.5" : ""}`}>
          {children}
        </div>
      </div>

      {onDismiss ? (
        <button
          type="button"
          onClick={onDismiss}
          className={`-mr-1 -mt-0.5 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-lg transition ${styles.dismiss}`}
          aria-label="Dismiss"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4" aria-hidden>
            <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
          </svg>
        </button>
      ) : null}
    </div>
  );
}
