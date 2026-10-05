import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from "react";
import AlertMessage from "../components/AlertMessage";
import ConfirmDialog from "../components/ConfirmDialog";

const FeedbackContext = createContext(null);

const DEFAULT_TOAST_MS = 4500;

export function FeedbackProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const [confirmState, setConfirmState] = useState(null);
  const confirmResolver = useRef(null);
  const toastTimers = useRef(new Map());

  const dismissToast = useCallback((id) => {
    const timer = toastTimers.current.get(id);
    if (timer) {
      clearTimeout(timer);
      toastTimers.current.delete(id);
    }
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const notify = useCallback(
    (input) => {
      const payload =
        typeof input === "string"
          ? { type: "info", message: input }
          : input || {};

      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const toast = {
        id,
        type: payload.type || "info",
        title: payload.title,
        message: payload.message || "",
        duration:
          payload.duration === 0
            ? 0
            : payload.duration || DEFAULT_TOAST_MS,
      };

      setToasts((prev) => [...prev, toast].slice(-4));

      if (toast.duration > 0) {
        const timer = setTimeout(() => dismissToast(id), toast.duration);
        toastTimers.current.set(id, timer);
      }

      return id;
    },
    [dismissToast]
  );

  const notifyApi = useMemo(
    () => ({
      show: notify,
      success: (message, opts = {}) =>
        notify({ type: "success", message, ...opts }),
      error: (message, opts = {}) =>
        notify({ type: "error", message, ...opts }),
      warning: (message, opts = {}) =>
        notify({ type: "warning", message, ...opts }),
      info: (message, opts = {}) =>
        notify({ type: "info", message, ...opts }),
    }),
    [notify]
  );

  const confirm = useCallback((options = {}) => {
    return new Promise((resolve) => {
      if (confirmResolver.current) {
        confirmResolver.current(false);
      }
      confirmResolver.current = resolve;
      setConfirmState({
        title: options.title || "Are you sure?",
        message: options.message || "",
        confirmLabel: options.confirmLabel || "Confirm",
        cancelLabel: options.cancelLabel || "Cancel",
        tone: options.tone || "default",
      });
    });
  }, []);

  const closeConfirm = useCallback((result) => {
    const resolve = confirmResolver.current;
    confirmResolver.current = null;
    setConfirmState(null);
    if (resolve) resolve(result);
  }, []);

  const value = useMemo(
    () => ({
      notify: notifyApi,
      confirm,
      dismissToast,
    }),
    [notifyApi, confirm, dismissToast]
  );

  return (
    <FeedbackContext.Provider value={value}>
      {children}

      <div
        className="pointer-events-none fixed inset-x-0 top-3 z-[90] flex flex-col items-center gap-2 px-4 sm:items-end sm:px-6"
        aria-live="polite"
        aria-relevant="additions"
      >
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto w-full max-w-md">
            <AlertMessage
              type={toast.type}
              title={toast.title}
              onDismiss={() => dismissToast(toast.id)}
            >
              {toast.message}
            </AlertMessage>
          </div>
        ))}
      </div>

      <ConfirmDialog
        open={Boolean(confirmState)}
        title={confirmState?.title}
        message={confirmState?.message}
        confirmLabel={confirmState?.confirmLabel}
        cancelLabel={confirmState?.cancelLabel}
        tone={confirmState?.tone}
        onConfirm={() => closeConfirm(true)}
        onCancel={() => closeConfirm(false)}
      />
    </FeedbackContext.Provider>
  );
}

export function useFeedback() {
  const ctx = useContext(FeedbackContext);
  if (!ctx) {
    throw new Error("useFeedback must be used within FeedbackProvider");
  }
  return ctx;
}
