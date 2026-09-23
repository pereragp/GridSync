import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";

export default function Login() {
  const { login, homePathFor } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
    <AuthShell>
      <PageHeader title="Sign in" subtitle="GridSync web console for Backoffice and Grid Operators." />
      {error ? <Alert>{error}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Email">
          <input className={inputClass} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <Field label="Password">
          <input className={inputClass} type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        <button className={`${btnPrimary} w-full`} disabled={loading}>
          {loading ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <div className="mt-4 space-y-1 text-sm text-slate-600">
        <p>
          <Link className="text-teal-700 hover:underline" to="/forgot-password">
            Forgot password?
          </Link>
        </p>
        <p>
          Prosumer test account?{" "}
          <Link className="text-teal-700 hover:underline" to="/register">
            Register
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}

export function AuthShell({ children }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-teal-50 to-slate-200 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <p className="text-2xl font-semibold tracking-tight text-teal-900">GridSync</p>
          <p className="text-sm text-slate-600">Smart Solar Microgrid Trading</p>
        </div>
        {children}
      </div>
    </div>
  );
}
