import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { completeReservation, verifyReservationQr } from '../api/reservations';
import {
  Alert,
  PageHeader,
  btnPrimary,
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
          setMessage('QR code captured. Verify the reservation to continue.');
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
      // The camera may already have stopped after a successful scan.
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

  return (
    <div>
      <PageHeader
        title='Verify reservation QR'
        subtitle='Scan the prosumer QR code or enter its payload to verify an approved booking.'
      />
      {error ? <Alert>{error}</Alert> : null}
      {message ? <Alert type='success'>{message}</Alert> : null}

      <div className='grid gap-6 lg:grid-cols-[minmax(0,24rem)_1fr]'>
        <section className={cardClass}>
          <h2 className='mb-3 text-lg font-medium'>Scan QR code</h2>
          <div
            id={scannerId}
            className='min-h-12 overflow-hidden rounded-md bg-slate-100'
          />
          <div className='mt-4 flex flex-wrap gap-2'>
            <button
              type='button'
              className={btnPrimary}
              onClick={startScanner}
              disabled={scanning}
            >
              Start camera
            </button>
            {scanning ? (
              <button
                type='button'
                className='rounded-md border border-slate-300 px-3 py-1.5 text-sm'
                onClick={stopScanner}
              >
                Stop camera
              </button>
            ) : null}
          </div>
          <form onSubmit={onVerify} className='mt-6'>
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
            <button
              className={`${btnPrimary} mt-3 w-full`}
              disabled={verifying || !payload.trim()}
            >
              {verifying ? 'Verifying...' : 'Verify reservation'}
            </button>
          </form>
        </section>

        <section className={cardClass}>
          <h2 className='mb-3 text-lg font-medium'>Verification result</h2>
          {result ? (
            <VerificationResult
              result={result}
              onComplete={onComplete}
              completing={completing}
            />
          ) : (
            <p className='text-sm text-slate-500'>
              No reservation verified yet.
            </p>
          )}
        </section>
      </div>
    </div>
  );
}

function VerificationResult({ result, onComplete, completing }) {
  return (
    <div className='space-y-3 text-sm'>
      <p className='font-semibold text-teal-800'>
        {result.status === 'Completed'
          ? 'Energy transfer completed'
          : 'Valid approved reservation'}
      </p>
      <Detail label='Reservation' value={result.reservationCode} />
      <Detail label='Prosumer NIC' value={result.prosumerNic} />
      <Detail label='Station' value={result.stationName || 'Unavailable'} />
      <Detail
        label='Time'
        value={`${formatDate(result.slotStart)} - ${formatDate(result.slotEnd)}`}
      />
      <Detail label='Type' value={result.reservationType === 'DropOff' ? 'Drop-off' : result.reservationType} />
      <Detail label='Energy' value={`${result.energyKwh} kWh`} />
      <p className='text-xs text-slate-500'>
        Completing applies actual energy on the battery (Charging increases
        stored kWh; Drop-off decreases it) and clears the reserved amount.
      </p>
      {result.status === 'Approved' ? (
        <button
          type='button'
          className={`${btnPrimary} mt-3 w-full`}
          onClick={onComplete}
          disabled={completing}
        >
          {completing ? 'Completing...' : 'Complete energy transfer'}
        </button>
      ) : null}
    </div>
  );
}

function Detail({ label, value }) {
  return (
    <p>
      <span className='font-medium text-slate-700'>{label}: </span>
      {value}
    </p>
  );
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}
