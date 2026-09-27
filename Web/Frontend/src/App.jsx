import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { FeedbackProvider } from "./context/FeedbackContext";
import { ProtectedRoute, PublicOnlyRoute } from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import BackofficeHome from "./pages/BackofficeHome";
import ChangePassword from "./pages/ChangePassword";
import CreateStaff from "./pages/CreateStaff";
import CreateStation from "./pages/CreateStation";
import ForgotPassword from "./pages/ForgotPassword";
import Login from "./pages/Login";
import OperatorHome from "./pages/OperatorHome";
import OperatorReservations from "./pages/OperatorReservations";
import OperatorQrScanner from './pages/OperatorQrScanner';
import Profile from "./pages/Profile";
import ProsumerHome from "./pages/ProsumerHome";
import RegisterProsumer from "./pages/RegisterProsumer";
import ResetPassword from "./pages/ResetPassword";
import Reservations from "./pages/Reservations";
import StationDetail from "./pages/StationDetail";
import Stations from "./pages/Stations";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <FeedbackProvider>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path='/login' element={<Login />} />
            <Route path='/register' element={<RegisterProsumer />} />
            <Route path='/forgot-password' element={<ForgotPassword />} />
            <Route path='/reset-password' element={<ResetPassword />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route element={<ProtectedRoute roles={["Backoffice"]} />}>
                <Route path="/backoffice" element={<BackofficeHome />} />
                <Route path="/backoffice/staff/new" element={<CreateStaff />} />
                <Route path="/stations/new" element={<CreateStation />} />
              </Route>
              <Route element={<ProtectedRoute roles={['GridOperator']} />}>
                <Route path='/operator' element={<OperatorHome />} />
              </Route>
              <Route
                element={
                  <ProtectedRoute
                    roles={["Backoffice", "GridOperator", "Prosumer"]}
                  />
                }
              >
                <Route path="/stations" element={<Stations />} />
                <Route path="/stations/:id" element={<StationDetail />} />
              </Route>
              <Route element={<ProtectedRoute roles={["Prosumer"]} />}>
                <Route path="/prosumer" element={<ProsumerHome />} />
              </Route>
              <Route path='/profile' element={<Profile />} />
              <Route path='/change-password' element={<ChangePassword />} />
              <Route
                path='/reservations'
                element={<ProtectedRoute roles={['Prosumer']} />}
              >
                <Route index element={<Reservations />} />
              </Route>
              <Route
                path="/backoffice"
                element={<ProtectedRoute roles={["Backoffice"]} />}
              >
                <Route index element={<BackofficeHome />} />
                <Route path="staff/new" element={<CreateStaff />} />
                <Route
                  path="reservations"
                  element={
                    <OperatorReservations
                      title="Approve energy reservations"
                      subtitle="Review prosumer Charging and Drop-off bookings. Approve to issue a QR, or reject with a reason."
                    />
                  }
                />
              </Route>
              <Route
                path='/operator'
                element={<ProtectedRoute roles={['GridOperator']} />}
              >
                <Route index element={<OperatorHome />} />
                <Route path='reservations' element={<OperatorReservations />} />
                <Route path='qr-scanner' element={<OperatorQrScanner />} />
              </Route>
            </Route>
          </Route>
          <Route path='*' element={<Navigate to='/login' replace />} />
        </Routes>
        </FeedbackProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
