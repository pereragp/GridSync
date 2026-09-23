import { useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword } from "../api/auth";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";
import { AuthShell } from "./Login";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

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
    <AuthShell>
      <PageHeader
        title="Forgot password"
        subtitle="We will email a reset link to your Gmail when email is enabled on the API."
      />
      {error ? <Alert>{error}</Alert> : null}
      {result ? (
        <Alert type="success">
          <p>{result.message}</p>
          {result.resetToken ? (
            <p className="mt-2 break-all text-xs">
              Dev fallback token: <strong>{result.resetToken}</strong>
            </p>
          ) : null}
        </Alert>
      ) : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Email">
          <input className={inputClass} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <button className={`${btnPrimary} w-full`} disabled={loading}>
          {loading ? "Sending…" : "Send reset link"}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Already have a link?{" "}
        <Link className="text-teal-700 hover:underline" to="/reset-password">
          Reset password
        </Link>
      </p>
    </AuthShell>
  );
}
