import { useEffect } from "react";

/**
 * Modal confirmation dialog.
 * Controlled via open / onConfirm / onCancel.
 */
export default function ConfirmDialog({
  open,
  title = "Are you sure?",
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "default",
  busy = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) return undefined;

    function onKeyDown(e) {
      if (busy) return;
      if (e.key === "Escape") onCancel?.();
      if (e.key === "Enter") onConfirm?.();
    }

    document.addEventListener("keydown", onKeyDown);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, busy, onCancel, onConfirm]);

  if (!open) return null;

  const isDanger = tone === "danger";

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center p-4 sm:items-center">
      <button
        type="button"
        aria-label="Close dialog"
        className="gs-dialog-backdrop absolute inset-0 bg-grid-900/45 backdrop-blur-[2px]"
        onClick={busy ? undefined : onCancel}
        disabled={busy}
      />

      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="gs-confirm-title"
        aria-describedby="gs-confirm-desc"
        className="gs-dialog-panel relative z-10 w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-grid-900/20"
      >
        <div className="border-b border-slate-100 bg-gradient-to-r from-grid-50 to-white px-5 py-4">
          <div className="flex items-start gap-3">
            <span
              className={`mt-0.5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${
                isDanger
                  ? "bg-red-100 text-red-700"
                  : "bg-grid-100 text-grid-700"
              }`}
            >
              {isDanger ? (
                <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5" aria-hidden>
                  <path
                    fillRule="evenodd"
                    d="M8.257 3.099c.765-1.36 2.721-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM10 7a.75.75 0 01.75.75v2.5a.75.75 0 01-1.5 0v-2.5A.75.75 0 0110 7zm0 6.25a.875.875 0 100-1.75.875.875 0 000 1.75z"
                    clipRule="evenodd"
                  />
                </svg>
              ) : (
                <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5" aria-hidden>
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-4.75a.75.75 0 001.5 0v-4.5a.75.75 0 00-1.5 0v4.5zM10 6.25a.875.875 0 100 1.75.875.875 0 000-1.75z"
                    clipRule="evenodd"
                  />
                </svg>
              )}
            </span>
            <div className="min-w-0">
              <h2
                id="gs-confirm-title"
                className="font-display text-lg font-semibold tracking-tight text-grid-900"
              >
                {title}
              </h2>
              {message ? (
                <p
                  id="gs-confirm-desc"
                  className="mt-1.5 text-sm leading-relaxed text-slate-600"
                >
                  {message}
                </p>
              ) : null}
            </div>
          </div>
        </div>

        <div className="flex flex-col-reverse gap-2 px-5 py-4 sm:flex-row sm:justify-end">
          <button
            type="button"
            disabled={busy}
            onClick={onCancel}
            className="inline-flex items-center justify-center rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            className={`inline-flex items-center justify-center rounded-xl px-4 py-2.5 text-sm font-semibold text-white transition disabled:opacity-60 ${
              isDanger
                ? "bg-red-700 hover:bg-red-800"
                : "bg-grid-700 hover:bg-grid-800"
            }`}
          >
            {busy ? "Working…" : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
