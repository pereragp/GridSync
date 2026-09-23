import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/auth";
import { Alert, Field, PageHeader, btnPrimary, cardClass, inputClass } from "../components/ui";
import { AuthShell } from "./Login";

export default function ResetPassword() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState(params.get("email") || "");
  const [resetToken, setResetToken] = useState(params.get("token") || "");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await resetPassword(email, resetToken, newPassword);
      setSuccess("Password reset. You can sign in now.");
      setTimeout(() => navigate("/login"), 1200);
    } catch (err) {
      setError(err.message || "Reset failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell>
      <PageHeader
        title="Reset password"
        subtitle="Open the link from your email, or paste the token if using the development fallback."
      />
      {error ? <Alert>{error}</Alert> : null}
      {success ? <Alert type="success">{success}</Alert> : null}
      <form onSubmit={onSubmit} className={cardClass}>
        <Field label="Email">
          <input className={inputClass} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <Field label="Reset token">
          <input className={inputClass} value={resetToken} onChange={(e) => setResetToken(e.target.value)} required />
        </Field>
        <Field label="New password">
          <input className={inputClass} type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
        </Field>
        <button className={`${btnPrimary} w-full`} disabled={loading}>
          {loading ? "Saving…" : "Reset password"}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        <Link className="text-teal-700 hover:underline" to="/login">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
