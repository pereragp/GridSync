import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Html5Qrcode } from 'html5-qrcode';
import { completeReservation, verifyReservationQr } from '../api/reservations';
import {
  Alert,
  btnPrimary,
  btnSecondary,
  cardClass,
  inputClass,
} from '../components/ui';

const scannerId = 'gridsync-qr-reader';

export default function OperatorQrScanner() {
  const scannerRef = useRef(null);
  const [payload, setPayload] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [scanning, setScanning] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [completing, setCompleting] = useState(false);

  useEffect(() => () => stopScanner(), []);

  async function startScanner() {
    setError('');
    setMessage('');
    setResult(null);
    if (scannerRef.current) return;

    const scanner = new Html5Qrcode(scannerId);
    scannerRef.current = scanner;
    try {
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 240, height: 240 } },
        (decodedText) => {
          setPayload(decodedText);
          setMessage('QR captured — verify the booking to continue.');
          stopScanner();
        },
        () => {},
      );
      setScanning(true);
    } catch (err) {
      scannerRef.current = null;
      setError(
        err.message || 'Camera could not be started. Use manual entry instead.',
      );
    }
  }

  async function stopScanner() {
    const scanner = scannerRef.current;
    scannerRef.current = null;
    setScanning(false);
    if (!scanner) return;
    try {
      if (scanner.isScanning) await scanner.stop();
      scanner.clear();
    } catch {
      // Camera may already have stopped after a successful scan.
    }
  }

  async function onVerify(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setResult(null);
    setVerifying(true);
    try {
      setResult(await verifyReservationQr(payload.trim()));
    } catch (err) {
      setError(err.message || 'QR verification failed');
    } finally {
      setVerifying(false);
    }
  }

  async function onComplete() {
    setError('');
    setMessage('');
    setCompleting(true);
    try {
      const completed = await completeReservation(result.reservationId);
      setResult((current) => ({ ...current, status: completed.status }));
      setMessage(`Transfer completed for ${completed.reservationCode}.`);
    } catch (err) {
      setError(err.message || 'Could not complete transfer');
    } finally {
      setCompleting(false);
    }
  }

  function onReset() {
    setPayload('');
    setResult(null);
    setError('');
    setMessage('');
  }

  return (
    <div className='space-y-6'>
      <section className='rounded-2xl border border-grid-200/80 bg-white px-5 py-6 shadow-sm sm:px-7'>
        <p className='text-xs font-semibold uppercase tracking-[0.18em] text-grid-600'>
          On-site transfer
        </p>
        <h1 className='mt-2 font-display text-3xl font-semibold tracking-tight text-grid-900'>
          Verify QR
        </h1>
        <p className='mt-2 max-w-2xl text-sm leading-relaxed text-slate-600'>
          Scan or paste the prosumer QR for an approved booking, then complete
          the energy transfer to update actual battery kWh.
        </p>
        <ol className='mt-5 flex flex-wrap gap-2 text-xs font-semibold text-slate-600'>
          <Step n={1} label='Scan or paste' />
          <Step n={2} label='Verify booking' />
          <Step n={3} label='Complete transfer' />
        </ol>
      </section>

      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type='success'>{message}</Alert> : null}

      <div className='grid gap-6 lg:grid-cols-[minmax(0,22rem)_1fr]'>
        <section className={`${cardClass} rounded-2xl`}>
          <h2 className='text-base font-semibold text-grid-900'>Scan code</h2>
          <p className='mt-1 text-sm text-slate-500'>
            Use the device camera or paste the QR payload below.
          </p>

          <div
            id={scannerId}
            className='mt-4 min-h-[12rem] overflow-hidden rounded-xl border border-slate-200 bg-slate-100'
          />

          <div className='mt-4 flex flex-wrap gap-2'>
            <button
              type='button'
              className={btnPrimary}
              onClick={startScanner}
              disabled={scanning}
            >
              {scanning ? 'Camera active…' : 'Start camera'}
            </button>
            {scanning ? (
              <button type='button' className={btnSecondary} onClick={stopScanner}>
                Stop
              </button>
            ) : null}
          </div>

          <form onSubmit={onVerify} className='mt-6 border-t border-slate-100 pt-5'>
            <label className='text-sm'>
              <span className='mb-1 block font-medium text-slate-700'>
                QR payload
              </span>
              <textarea
                className={`${inputClass} min-h-24 resize-y`}
                value={payload}
                onChange={(event) => setPayload(event.target.value)}
                placeholder='Scan a code or paste its payload'
                required
              />
            </label>
            <div className='mt-3 flex flex-wrap gap-2'>
              <button
                className={`${btnPrimary} flex-1`}
                disabled={verifying || !payload.trim()}
              >
                {verifying ? 'Verifying…' : 'Verify booking'}
              </button>
              {(payload || result) && (
                <button type='button' className={btnSecondary} onClick={onReset}>
                  Clear
                </button>
              )}
            </div>
          </form>
        </section>

        <section className={`${cardClass} rounded-2xl`}>
          <h2 className='text-base font-semibold text-grid-900'>
            Verification result
          </h2>
          {result ? (
            <VerificationResult
              result={result}
              onComplete={onComplete}
              completing={completing}
            />
          ) : (
            <div className='mt-6 rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center'>
              <p className='text-sm text-slate-500'>
                No booking verified yet. Scan a QR or paste a payload to begin.
              </p>
              <Link
                to='/operator'
                className='mt-4 inline-flex text-sm font-semibold text-grid-700 hover:underline'
              >
                ← Back to booking queue
              </Link>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Step({ n, label }) {
  return (
    <li className='inline-flex items-center gap-2 rounded-full bg-grid-50 px-3 py-1.5 ring-1 ring-grid-100'>
      <span className='flex h-5 w-5 items-center justify-center rounded-full bg-grid-700 text-[10px] text-white'>
        {n}
      </span>
      {label}
    </li>
  );
}

function VerificationResult({ result, onComplete, completing }) {
  const done = result.status === 'Completed';
  const typeLabel =
    result.reservationType === 'DropOff' ? 'Drop-off' : result.reservationType;

  return (
    <div className='mt-4 space-y-4'>
      <div
        className={`rounded-xl px-4 py-3 text-sm font-semibold ${
          done
            ? 'bg-grid-100 text-grid-900'
            : 'bg-emerald-50 text-emerald-900'
        }`}
      >
        {done
          ? 'Energy transfer completed'
          : 'Valid approved reservation — ready to complete'}
      </div>

      <dl className='grid gap-3 sm:grid-cols-2'>
        <Detail label='Reservation' value={result.reservationCode} />
        <Detail label='Prosumer NIC' value={result.prosumerNic} />
        <Detail label='Station' value={result.stationName || 'Unavailable'} />
        <Detail label='Type' value={typeLabel} />
        <Detail
          label='Window'
          value={`${formatDate(result.slotStart)} – ${formatDate(result.slotEnd)}`}
        />
        <Detail label='Energy' value={`${result.energyKwh} kWh`} />
      </dl>

      <p className='rounded-lg bg-slate-50 px-3 py-2 text-xs leading-relaxed text-slate-600'>
        Completing applies actual energy on the battery (Charging increases
        stored kWh; Drop-off decreases it) and clears the reserved amount.
      </p>

      {result.status === 'Approved' ? (
        <button
          type='button'
          className={`${btnPrimary} w-full py-2.5`}
          onClick={onComplete}
          disabled={completing}
        >
          {completing ? 'Completing…' : 'Complete energy transfer'}
        </button>
      ) : null}
    </div>
  );
}

function Detail({ label, value }) {
  return (
    <div className='rounded-lg border border-slate-100 bg-slate-50/80 px-3 py-2.5'>
      <dt className='text-[11px] font-semibold uppercase tracking-wide text-slate-500'>
        {label}
      </dt>
      <dd className='mt-0.5 text-sm font-medium text-slate-900'>{value}</dd>
    </div>
  );
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
