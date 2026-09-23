export function PageHeader({ title, subtitle }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{title}</h1>
      {subtitle ? <p className="mt-1 text-sm text-slate-600">{subtitle}</p> : null}
    </div>
  );
}

export function Alert({ type = "error", children }) {
  const styles =
    type === "success"
      ? "border-teal-200 bg-teal-50 text-teal-900"
      : "border-red-200 bg-red-50 text-red-800";
  return (
    <div className={`mb-4 rounded-md border px-3 py-2 text-sm ${styles}`}>{children}</div>
  );
}

export function Field({ label, children }) {
  return (
    <label className="mb-3 block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{label}</span>
      {children}
    </label>
  );
}

export const inputClass =
  "w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-teal-600 focus:ring-1 focus:ring-teal-600";

export const btnPrimary =
  "inline-flex items-center justify-center rounded-md bg-teal-700 px-4 py-2 text-sm font-medium text-white hover:bg-teal-800 disabled:opacity-60";

export const btnSecondary =
  "inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-60";

export const cardClass = "rounded-lg border border-slate-200 bg-white p-5 shadow-sm";
