import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword } from "../api/auth";
import BrandLogo from "../components/BrandLogo";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=2000&q=80";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
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

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setResult(null);
    setLoading(true);
    try {
      const data = await forgotPassword(email);
      setResult(data);
    } catch (err) {
      setError(err.message || "Request failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative min-h-dvh bg-grid-900 text-white">
      <div className="pointer-events-none fixed inset-0 gs-animate-fade-in">
        <img
          src={HERO_IMAGE}
          alt="Power lines against a renewable energy sky"
          className="h-full w-full object-cover gs-animate-pan"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-900/75 to-grid-800/45" />
        <div className="absolute inset-0 bg-gradient-to-t from-grid-900/80 via-transparent to-grid-900/30" />
      </div>

      <div className="relative z-10 mx-auto flex min-h-dvh max-w-7xl flex-col px-5 py-6 sm:px-8 lg:px-12">
        <div className="grid flex-1 items-center gap-10 py-6 lg:grid-cols-12 lg:gap-16">
          <section
            className="gs-animate-fade-up lg:col-span-7"
            style={{ animationDelay: "0.12s" }}
          >
            <BrandLogo variant="hero" className="mb-4 drop-shadow-md sm:mb-5" />
            <h1 className="max-w-xl font-display text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
              Regain access in a few steps.
            </h1>
            <p className="mt-5 max-w-lg text-base leading-relaxed text-grid-100/85 sm:text-lg">
              Enter the email on your GridSync account. We will send a secure
              reset link so you can choose a new password.
            </p>
            <ol className="mt-8 max-w-md space-y-3 text-sm text-grid-100/80">
              <Step n={1} text="Enter your account email" />
              <Step n={2} text="Open the reset link from your inbox" />
              <Step n={3} text="Choose a new password and sign in" />
            </ol>
          </section>

          <section
            className="gs-animate-fade-up lg:col-span-5"
            style={{ animationDelay: "0.22s" }}
          >
            <div className="rounded-2xl border border-white/15 bg-white/95 p-6 text-slate-900 shadow-2xl shadow-grid-900/30 backdrop-blur-md sm:p-8">
              <div className="mb-6">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-grid-600">
                  Account recovery
                </p>
                <h2 className="mt-2 text-2xl font-semibold tracking-tight text-grid-900">
                  Forgot password
                </h2>
                <p className="mt-1 text-sm text-slate-600">
                  We will email a reset link when mail is configured on the API.
                </p>
              </div>

              {error ? (
                <div
                  role="alert"
                  className="mb-4 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm text-red-800"
                >
                  {error}
                </div>
              ) : null}

              {result ? (
                <div
                  role="status"
                  className="mb-4 rounded-xl border border-grid-200 bg-grid-50 px-3.5 py-3 text-sm text-grid-900"
                >
                  <p className="font-semibold">Check your inbox</p>
                  <p className="mt-1 text-grid-800/90">{result.message}</p>
                  {result.resetToken ? (
                    <p className="mt-3 break-all rounded-lg bg-white/80 px-2.5 py-2 text-xs text-slate-600">
                      Dev fallback token:{" "}
                      <strong className="text-slate-800">{result.resetToken}</strong>
                    </p>
                  ) : null}
                  <div className="mt-4 flex flex-wrap gap-2">
                    <Link
                      to={`/reset-password${email ? `?email=${encodeURIComponent(email)}` : ""}`}
                      className="inline-flex rounded-lg bg-grid-700 px-3.5 py-2 text-sm font-semibold text-white transition hover:bg-grid-800"
                    >
                      Continue to reset
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        setResult(null);
                        setEmail("");
                      }}
                      className="inline-flex rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                    >
                      Send again
                    </button>
                  </div>
                </div>
              ) : null}

              {!result ? (
                <form onSubmit={onSubmit} className="space-y-4">
                  <label className="block text-sm">
                    <span className="mb-1.5 block font-medium text-slate-700">
                      Email
                    </span>
                    <input
                      className={inputClass}
                      type="email"
                      autoComplete="email"
                      placeholder="you@gridsync.local"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                  </label>

                  <button
                    type="submit"
                    disabled={loading}
                    className="inline-flex w-full items-center justify-center rounded-lg bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {loading ? "Sending…" : "Send reset link"}
                  </button>
                </form>
              ) : null}

              <div className="mt-6 space-y-2 border-t border-slate-200 pt-4 text-center text-sm text-slate-600">
                <p>
                  Already have a token?{" "}
                  <Link
                    to="/reset-password"
                    className="font-medium text-grid-700 hover:underline"
                  >
                    Reset password
                  </Link>
                </p>
                <p>
                  Remembered it?{" "}
                  <Link
                    to="/login"
                    className="font-medium text-grid-700 hover:underline"
                  >
                    Back to sign in
                  </Link>
                </p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

function Step({ n, text }) {
  return (
    <li className="flex items-start gap-3">
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white/15 text-xs font-bold text-white ring-1 ring-white/20">
        {n}
      </span>
      <span className="pt-0.5">{text}</span>
    </li>
  );
}

const inputClass =
  "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/25";
