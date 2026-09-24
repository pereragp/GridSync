import { useState } from "react";
import { Link } from "react-router-dom";
import { createStaff } from "../api/users";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?auto=format&fit=crop&w=1800&q=80";

const ROLES = [
  {
    value: "Backoffice",
    title: "Backoffice",
    detail: "Full system administration, prosumer approvals, and account control.",
  },
  {
    value: "GridOperator",
    title: "Grid Operator",
    detail: "Operational access for stations, slots, bookings, and on-site verification.",
  },
];

export default function CreateStaff() {
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    password: "",
    role: "GridOperator",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [created, setCreated] = useState(null);
  const [loading, setLoading] = useState(false);

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setCreated(null);
    setLoading(true);
    try {
      const user = await createStaff(form);
      setCreated(user);
      setSuccess(`${user.role} account created for ${user.fullName}.`);
      setForm({
        fullName: "",
        email: "",
        phone: "",
        password: "",
        role: "GridOperator",
      });
    } catch (err) {
      setError(err.message || "Create failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-2xl border border-grid-800/10 shadow-lg shadow-grid-900/10">
        <div className="absolute inset-0">
          <img
            src={HERO_IMAGE}
            alt="Solar panels under open sky"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-800/80 to-grid-700/40" />
        </div>

        <div className="relative z-10 flex flex-col gap-4 px-6 py-10 sm:px-10 sm:py-12 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-grid-100/70">
              Backoffice console
            </p>
            <h1 className="mt-3 font-display text-3xl font-semibold leading-tight text-white sm:text-4xl">
              Create staff user
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-relaxed text-grid-100/85 sm:text-base">
              Provision Backoffice or Grid Operator accounts with secure credentials
              and role-based access to GridSync.
            </p>
          </div>
          <Link
            to="/backoffice"
            className="inline-flex items-center justify-center rounded-xl border border-white/25 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20"
          >
            Back to users
          </Link>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-12">
        {/* Role picker */}
        <aside className="space-y-4 lg:col-span-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Select role
            </h2>
            <div className="mt-4 space-y-3">
              {ROLES.map((role) => {
                const active = form.role === role.value;
                return (
                  <button
                    key={role.value}
                    type="button"
                    onClick={() => set("role", role.value)}
                    className={`w-full rounded-xl border px-4 py-3 text-left transition ${
                      active
                        ? "border-grid-600 bg-grid-50 shadow-sm ring-2 ring-grid-500/20"
                        : "border-slate-200 bg-white hover:border-grid-300"
                    }`}
                  >
                    <p className={`font-semibold ${active ? "text-grid-900" : "text-slate-800"}`}>
                      {role.title}
                    </p>
                    <p className="mt-1 text-xs leading-relaxed text-slate-600">{role.detail}</p>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="rounded-2xl border border-grid-100 bg-gradient-to-br from-grid-50 to-white p-5">
            <p className="text-sm font-semibold text-grid-900">Password rules</p>
            <ul className="mt-2 space-y-1.5 text-xs text-slate-600">
              <li>At least 8 characters</li>
              <li>Include at least one letter</li>
              <li>Include at least one number</li>
            </ul>
          </div>
        </aside>

        {/* Form */}
        <section className="lg:col-span-8">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <div className="mb-6">
              <h2 className="text-xl font-semibold tracking-tight text-grid-900">
                Account details
              </h2>
              <p className="mt-1 text-sm text-slate-600">
                Creating a <span className="font-semibold text-grid-800">{form.role}</span> user.
                New staff accounts are activated immediately.
              </p>
            </div>

            {error ? (
              <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                {error}
              </div>
            ) : null}
            {success ? (
              <div className="mb-4 rounded-xl border border-grid-200 bg-grid-50 px-4 py-3 text-sm text-grid-800">
                <p>{success}</p>
                {created ? (
                  <p className="mt-1 text-xs text-grid-700/80">
                    ID · {created.id} · Status · {created.status}
                  </p>
                ) : null}
              </div>
            ) : null}

            <form onSubmit={onSubmit} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-slate-700">Full name</span>
                  <input
                    className={inputClass}
                    value={form.fullName}
                    onChange={(e) => set("fullName", e.target.value)}
                    placeholder="e.g. Nimal Perera"
                    required
                  />
                </label>

                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Email</span>
                  <input
                    className={inputClass}
                    type="email"
                    value={form.email}
                    onChange={(e) => set("email", e.target.value)}
                    placeholder="operator@gridsync.local"
                    required
                  />
                </label>

                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Phone</span>
                  <input
                    className={inputClass}
                    value={form.phone}
                    onChange={(e) => set("phone", e.target.value)}
                    placeholder="07xxxxxxxx"
                    required
                  />
                </label>

                <label className="block text-sm sm:col-span-2">
                  <span className="mb-1.5 block font-medium text-slate-700">Temporary password</span>
                  <div className="relative">
                    <input
                      className={`${inputClass} pr-16`}
                      type={showPassword ? "text" : "password"}
                      value={form.password}
                      onChange={(e) => set("password", e.target.value)}
                      placeholder="Min 8 chars, letter + number"
                      required
                    />
                    <button
                      type="button"
                      className="absolute inset-y-0 right-0 px-3 text-xs font-medium text-grid-700 hover:text-grid-900"
                      onClick={() => setShowPassword((v) => !v)}
                    >
                      {showPassword ? "Hide" : "Show"}
                    </button>
                  </div>
                </label>
              </div>

              <div className="flex flex-wrap items-center gap-3 pt-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="inline-flex rounded-xl bg-grid-700 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loading ? "Creating account…" : `Create ${form.role}`}
                </button>
                <Link
                  to="/backoffice"
                  className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                >
                  Cancel
                </Link>
              </div>
            </form>
          </div>
        </section>
      </div>
    </div>
  );
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20";
