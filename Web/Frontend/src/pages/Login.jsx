import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import BrandLogo from "../components/BrandLogo";

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=2000&q=80";

export default function Login() {
  const { login, homePathFor } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Prevent light body color flashing on overscroll.
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
    setLoading(true);
    try {
      const session = await login(email, password);
      navigate(homePathFor(session.role));
    } catch (err) {
      setError(err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative min-h-dvh bg-grid-900 text-white">
      {/* Fixed full-viewport background so scroll/overscroll never shows white edges */}
      <div className="pointer-events-none fixed inset-0 gs-animate-fade-in">
        <img
          src={HERO_IMAGE}
          alt="Solar panels generating renewable energy in an open field"
          className="h-full w-full object-cover gs-animate-pan"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-grid-900/92 via-grid-900/75 to-grid-800/45" />
        <div className="absolute inset-0 bg-gradient-to-t from-grid-900/80 via-transparent to-grid-900/30" />
      </div>

      <div className="relative z-10 mx-auto flex min-h-dvh max-w-7xl flex-col px-5 py-6 sm:px-8 lg:px-12">
        <div className="grid flex-1 items-center gap-10 py-6 lg:grid-cols-12 lg:gap-16">
          {/* Brand / story column */}
          <section className="gs-animate-fade-up lg:col-span-7" style={{ animationDelay: "0.12s" }}>
            <BrandLogo variant="hero" className="mb-4 drop-shadow-md sm:mb-5" />
            <h1 className="max-w-xl font-display text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
              Power trading, synchronized for a cleaner grid.
            </h1>
            <p className="mt-5 max-w-lg text-base leading-relaxed text-grid-100/85 sm:text-lg">
              Sign in to manage microgrid nodes, energy slots, and prosumer
              reservations from one secure console.
            </p>
          </section>

          {/* Sign-in panel */}
          <section
            className="gs-animate-fade-up lg:col-span-5"
            style={{ animationDelay: "0.22s" }}
          >
            <div className="rounded-2xl border border-white/15 bg-white/95 p-6 text-slate-900 shadow-2xl shadow-grid-900/30 backdrop-blur-md sm:p-8">
              <div className="mb-6">
                <h2 className="text-2xl font-semibold tracking-tight text-grid-900">
                  Welcome back
                </h2>
                <p className="mt-1 text-sm text-slate-600">
                  Use your GridSync staff credentials to continue.
                </p>
              </div>

              {error ? (
                <div
                  role="alert"
                  className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
                >
                  {error}
                </div>
              ) : null}

              <form onSubmit={onSubmit} className="space-y-4">
                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Email</span>
                  <input
                    className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/25"
                    type="email"
                    autoComplete="email"
                    placeholder="you@gridsync.local"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </label>

                <label className="block text-sm">
                  <span className="mb-1.5 block font-medium text-slate-700">Password</span>
                  <div className="relative">
                    <input
                      className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 pr-16 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/25"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
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

                <div className="flex justify-end">
                  <Link
                    to="/forgot-password"
                    className="text-sm font-medium text-grid-700 hover:text-grid-900 hover:underline"
                  >
                    Forgot password?
                  </Link>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="inline-flex w-full items-center justify-center rounded-lg bg-grid-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-grid-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loading ? "Signing in…" : "Sign in"}
                </button>
              </form>

              <p className="mt-6 border-t border-slate-200 pt-4 text-center text-sm text-slate-600">
                Prosumer test account?{" "}
                <Link to="/register" className="font-medium text-grid-700 hover:underline">
                  Register here
                </Link>
              </p>
            </div>
          </section>
        </div>

        <footer className="gs-animate-fade-up pb-2 text-xs text-grid-100/55" style={{ animationDelay: "0.35s" }}>
        </footer>
      </div>
    </div>
  );
}

/** Shared shell for other auth pages until they are redesigned page-by-page. */
export function AuthShell({ children }) {
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

  return (
    <div className="relative min-h-dvh bg-grid-900">
      <div className="pointer-events-none fixed inset-0">
        <img
          src={HERO_IMAGE}
          alt=""
          className="h-full w-full object-cover opacity-40"
        />
        <div className="absolute inset-0 bg-grid-900/80" />
      </div>
      <div className="relative z-10 mx-auto flex min-h-dvh max-w-md flex-col justify-center px-5 py-10">
        <div className="mb-6 flex justify-center">
          <BrandLogo variant="auth" className="drop-shadow-md" />
        </div>
        <div className="rounded-2xl border border-white/10 bg-white p-6 shadow-xl sm:p-7">
          {children}
        </div>
      </div>
    </div>
  );
}
