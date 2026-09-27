import { useCallback, useEffect, useRef, useState } from "react";
import {
  Autocomplete,
  GoogleMap,
  Marker,
  useJsApiLoader,
} from "@react-google-maps/api";

/** Default map center — Colombo, Sri Lanka */
export const DEFAULT_MAP_CENTER = { lat: 6.9271, lng: 79.8612 };

const MAP_LIBRARIES = ["places"];

const mapContainerStyle = { width: "100%", height: "100%" };

/**
 * Interactive (or read-only) Google Map for picking a station location.
 * Click the map or drag the pin; search uses Places Autocomplete.
 *
 * Requires VITE_GOOGLE_MAPS_API_KEY with Maps JavaScript API + Places API enabled.
 */
export default function LocationPicker({
  latitude,
  longitude,
  onChange,
  readOnly = false,
  heightClass = "h-72",
}) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || "";

  const { isLoaded, loadError } = useJsApiLoader({
    id: "gridsync-google-maps",
    googleMapsApiKey: apiKey,
    libraries: MAP_LIBRARIES,
  });

  const hasValue =
    latitude !== "" &&
    latitude != null &&
    longitude !== "" &&
    longitude != null &&
    !Number.isNaN(Number(latitude)) &&
    !Number.isNaN(Number(longitude));

  const position = hasValue
    ? { lat: Number(latitude), lng: Number(longitude) }
    : null;

  const center = position || DEFAULT_MAP_CENTER;
  const mapRef = useRef(null);
  const autocompleteRef = useRef(null);
  const [mapReady, setMapReady] = useState(false);
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState("");

  const onMapLoad = useCallback((map) => {
    mapRef.current = map;
    setMapReady(true);
  }, []);

  const onMapUnmount = useCallback(() => {
    mapRef.current = null;
    setMapReady(false);
  }, []);

  useEffect(() => {
    if (mapRef.current && position) {
      mapRef.current.panTo(position);
    }
  }, [position?.lat, position?.lng]);

  function emitLatLng(lat, lng, label) {
    onChange?.({
      latitude: roundCoord(lat),
      longitude: roundCoord(lng),
      ...(label ? { label } : {}),
    });
  }

  function useCurrentLocation() {
    setLocationError("");

    if (!navigator.geolocation) {
      setLocationError("Geolocation is not supported by this browser.");
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        emitLatLng(lat, lng);
        if (mapRef.current) {
          mapRef.current.panTo({ lat, lng });
          mapRef.current.setZoom(16);
        }
        setLocating(false);
      },
      (err) => {
        const messages = {
          1: "Location permission denied. Allow access in your browser settings.",
          2: "Location unavailable. Try again or pick on the map.",
          3: "Location request timed out. Try again.",
        };
        setLocationError(messages[err.code] || "Could not get your current location.");
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
    );
  }

  function onMapClick(e) {
    if (readOnly || !e.latLng) return;
    setLocationError("");
    emitLatLng(e.latLng.lat(), e.latLng.lng());
  }

  function onMarkerDragEnd(e) {
    if (!e.latLng) return;
    setLocationError("");
    emitLatLng(e.latLng.lat(), e.latLng.lng());
  }

  function onPlaceChanged() {
    const place = autocompleteRef.current?.getPlace();
    if (!place?.geometry?.location) return;

    const lat = place.geometry.location.lat();
    const lng = place.geometry.location.lng();
    setLocationError("");
    emitLatLng(lat, lng, place.formatted_address || place.name);

    if (mapRef.current) {
      mapRef.current.panTo({ lat, lng });
      mapRef.current.setZoom(15);
    }
  }

  if (!apiKey) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
        <p className="font-semibold">Google Maps API key missing</p>
        <p className="mt-1 text-xs leading-relaxed text-amber-900/80">
          Add <code className="rounded bg-amber-100 px-1">VITE_GOOGLE_MAPS_API_KEY</code>{" "}
          to <code className="rounded bg-amber-100 px-1">Web/Frontend/.env</code>, enable
          the <strong>Maps JavaScript API</strong> and <strong>Places API</strong>, then
          restart the Vite dev server.
        </p>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
        Failed to load Google Maps. Check your API key and that Maps JavaScript API is
        enabled.
      </div>
    );
  }

  if (!isLoaded) {
    return (
      <div
        className={`flex items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-sm text-slate-500 ${heightClass}`}
      >
        Loading Google Maps…
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {!readOnly ? (
        <div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <div className="min-w-0 flex-1">
              <Autocomplete
                onLoad={(ac) => {
                  autocompleteRef.current = ac;
                }}
                onPlaceChanged={onPlaceChanged}
                options={{
                  componentRestrictions: { country: "lk" },
                  fields: ["formatted_address", "geometry", "name"],
                }}
              >
                <input
                  type="search"
                  placeholder="Search place in Sri Lanka…"
                  className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm outline-none transition focus:border-grid-600 focus:ring-2 focus:ring-grid-500/20"
                />
              </Autocomplete>
            </div>
            <button
              type="button"
              onClick={useCurrentLocation}
              disabled={locating}
              className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-grid-300 bg-white px-4 py-2.5 text-sm font-semibold text-grid-800 transition hover:border-grid-500 hover:bg-grid-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <CrosshairIcon />
              {locating ? "Locating…" : "Use my location"}
            </button>
          </div>
          {locationError ? (
            <p className="mt-2 text-xs text-red-700">{locationError}</p>
          ) : null}
          <p className="mt-2 text-xs text-slate-500">
            Search a place, use your current location, click the map, or drag the
            marker.
          </p>
        </div>
      ) : null}

      <div
        className={`overflow-hidden rounded-xl border border-slate-200 ${heightClass}`}
      >
        <GoogleMap
          mapContainerStyle={mapContainerStyle}
          center={center}
          zoom={position ? 14 : 11}
          onLoad={onMapLoad}
          onUnmount={onMapUnmount}
          onClick={onMapClick}
          options={{
            streetViewControl: false,
            mapTypeControl: false,
            fullscreenControl: true,
            clickableIcons: false,
            draggable: true,
            scrollwheel: !readOnly,
            disableDoubleClickZoom: readOnly,
            gestureHandling: readOnly ? "cooperative" : "greedy",
          }}
        >
          {position && mapReady ? (
            <Marker
              position={position}
              draggable={!readOnly}
              onDragEnd={onMarkerDragEnd}
            />
          ) : null}
        </GoogleMap>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            Latitude
          </p>
          <p className="mt-0.5 font-mono text-sm text-slate-800">
            {hasValue ? Number(latitude).toFixed(6) : "Not selected"}
          </p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            Longitude
          </p>
          <p className="mt-0.5 font-mono text-sm text-slate-800">
            {hasValue ? Number(longitude).toFixed(6) : "Not selected"}
          </p>
        </div>
      </div>
    </div>
  );
}

function roundCoord(n) {
  return Math.round(n * 1e6) / 1e6;
}

function CrosshairIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
    </svg>
  );
}
