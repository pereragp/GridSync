import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function ProtectedRoute({ roles }) {
  const { isAuthenticated, user, homePathFor } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles?.length && !roles.includes(user.role)) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }

  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { isAuthenticated, user, homePathFor } = useAuth();

  if (isAuthenticated) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }

  return <Outlet />;
}
