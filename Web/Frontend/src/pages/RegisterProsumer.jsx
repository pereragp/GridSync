import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { registerProsumer } from "../api/users";
import BrandLogo from "../components/BrandLogo";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=2000&q=80";

export default function RegisterProsumer() {
  const [form, setForm] = useState({
    nic: "",
    fullName: "",
    email: "",
    phone: "",
    password: "",
    address: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const html = document.documentElement;
    const body = document.body;
    const prevHtml = html.style.backgroundColor;
    const prevBody = body.style.backgroundColor;
    html.style.backgroundColor = "#0c3d25";
    body.style.backgroundColor = "#0c3d25";
    return () => {
      html.style.backgroundColor = prevHtml;
      body.style.backgroundColor = prevBody;
    };
  }, []);

  function set(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await registerProsumer(form);
      setSuccess(
        "Registered successfully. Your account is Pending until a Backoffice officer approves it.",
      );
      setForm({
        nic: "",
        fullName: "",
        email: "",
        phone: "",
        password: "",
        address: "",
      });
    } catch (err) {
      setError(err.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative min-h-dvh bg-grid-900 text-white">
      <div className="pointer-events-none fixed inset-0 gs-animate-fade-in">
        <img
          src={HERO_IMAGE}
          alt="Solar panels generating renewable energy"
          className="h-full w-full object-cover gs-animate-pan"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-900/75 to-grid-800/45" />
        <div className="absolute inset-0 bg-gradient-to-t from-grid-900/80 via-transparent to-grid-900/30" />
      </div>

      <div className="relative z-10 mx-auto flex min-h-dvh max-w-7xl flex-col px-5 py-6 sm:px-8 lg:px-12">
        <div className="grid flex-1 items-center gap-10 py-6 lg:grid-cols-12 lg:gap-14">
          <section
            className="gs-animate-fade-up lg:col-span-5"
            style={{ animationDelay: "0.12s" }}
          >
            <BrandLogo variant="hero" className="mb-4 drop-shadow-md sm:mb-5" />
            <h1 className="max-w-lg font-display text-4xl font-semibold leading-tight text-white sm:text-5xl">
              Join GridSync as a solar prosumer.
            </h1>
            <p className="mt-5 max-w-md text-base leading-relaxed text-grid-100/85 sm:text-lg">
              Register with your NIC to reserve Charging and Drop-off slots once
              Backoffice activates your account.
            </p>
            <ul className="mt-8 max-w-md space-y-3 text-sm text-grid-100/80">
              <Benefit text="NIC is your primary account key" />
              <Benefit text="Status starts as Pending after signup" />
              <Benefit text="Book energy slots from the mobile app after approval" />
            </ul>
          </section>

          <section
            className="gs-animate-fade-up lg:col-span-7"
            style={{ animationDelay: "0.22s" }}
          >
            <div className="rounded-2xl border border-white/15 bg-white/95 p-6 text-slate-900 shadow-2xl shadow-grid-900/30 backdrop-blur-md sm:p-8">
              <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-grid-600">
                    Prosumer signup
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold tracking-tight text-grid-900">
                    Create your account
                  </h2>
                  <p className="mt-1 text-sm text-slate-600">
                    Takes a minute. Approval is required before you can book.
                  </p>
                </div>
                <Link
                  to="/login"
                  className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                >
                  Sign in
                </Link>
              </div>

              {error ? (
                <div
                  role="alert"
                  className="mb-4 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm text-red-800"
                >
                  {error}
                </div>
              ) : null}

              {success ? (
                <div
                  role="status"
                  className="mb-4 rounded-xl border border-grid-200 bg-grid-50 px-3.5 py-3 text-sm text-grid-900"
                >
                  <p className="font-semibold">You are registered</p>
                  <p className="mt-1 text-grid-800/90">{success}</p>
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Link
                      to="/login"
                      className="inline-flex rounded-lg bg-grid-700 px-3.5 py-2 text-sm font-semibold text-white transition hover:bg-grid-800"
                    >
                      Go to sign in
                    </Link>
                    <button
                      type="button"
                      onClick={() => setSuccess("")}
                      className="inline-flex rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                    >
                      Register another
                    </button>
                  </div>
                </div>
              ) : null}

              {!success ? (
                <form onSubmit={onSubmit} className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        National Identity Card (NIC)
                      </span>
                      <input
                        className={inputClass}
                        value={form.nic}
                        onChange={(e) => set("nic", e.target.value)}
                        placeholder="e.g. 199012345678"
                        autoComplete="off"
                        required
                      />
                      <span className="mt-1 block text-xs text-slate-500">
                        Used as your unique prosumer key across GridSync.
                      </span>
                    </label>

                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Full name
                      </span>
                      <input
                        className={inputClass}
                        value={form.fullName}
                        onChange={(e) => set("fullName", e.target.value)}
                        placeholder="Your full name"
                        autoComplete="name"
                        required
                      />
                    </label>

                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Email
                      </span>
                      <input
                        className={inputClass}
                        type="email"
                        value={form.email}
                        onChange={(e) => set("email", e.target.value)}
                        placeholder="you@example.com"
                        autoComplete="email"
                        required
                      />
                    </label>

                    <label className="block text-sm">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Phone
                      </span>
                      <input
                        className={inputClass}
                        value={form.phone}
                        onChange={(e) => set("phone", e.target.value)}
                        placeholder="07xxxxxxxx"
                        autoComplete="tel"
                        required
                      />
                    </label>

                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Address
                        <span className="ml-1 font-normal text-slate-400">
                          (optional)
                        </span>
                      </span>
                      <input
                        className={inputClass}
                        value={form.address}
                        onChange={(e) => set("address", e.target.value)}
                        placeholder="Street, city"
                        autoComplete="street-address"
                      />
                    </label>

                    <label className="block text-sm sm:col-span-2">
                      <span className="mb-1.5 block font-medium text-slate-700">
                        Password
                      </span>
                      <div className="relative">
                        <input
                          className={`${inputClass} pr-16`}
                          type={showPassword ? "text" : "password"}
                          value={form.password}
                          onChange={(e) => set("password", e.target.value)}
                          placeholder="Min 8 characters, letter + number"
                          autoComplete="new-password"
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
                      <p className="mt-1.5 text-xs text-slate-500">
                        At least 8 characters with one letter and one number.
                      </p>
                    </label>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="inline-flex w-full items-center justify-center rounded-lg bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {loading ? "Creating account…" : "Create prosumer account"}
                  </button>
                </form>
              ) : null}

              {!success ? (
                <p className="mt-6 border-t border-slate-200 pt-4 text-center text-sm text-slate-600">
                  Already registered?{" "}
                  <Link
                    to="/login"
                    className="font-medium text-grid-700 hover:underline"
                  >
                    Sign in
                  </Link>
                </p>
              ) : null}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

function Benefit({ text }) {
  return (
    <li className="flex items-start gap-3">
      <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-400/25 text-emerald-100 ring-1 ring-emerald-300/30">
        <svg viewBox="0 0 20 20" fill="currentColor" className="h-3 w-3" aria-hidden>
          <path
            fillRule="evenodd"
            d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
            clipRule="evenodd"
          />
        </svg>
      </span>
      <span>{text}</span>
    </li>
  );
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/25";
