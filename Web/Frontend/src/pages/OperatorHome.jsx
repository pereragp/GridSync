import { PageHeader, cardClass } from "../components/ui";
import { useAuth } from "../context/AuthContext";

export default function OperatorHome() {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader
        title={`Welcome, ${user.fullName}`}
        subtitle="Grid Operator console. Station and booking tools will appear here next."
      />
      <div className={cardClass}>
        <p className="text-sm text-slate-600">
          You are signed in as <strong>{user.role}</strong>. Use Profile or Password in the top nav for account settings.
        </p>
      </div>
    </div>
  );
}
