const isDev = import.meta.env?.DEV || false;

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (isDev 
    ? "http://localhost:8080" 
    : window.location.origin);

export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || (isDev 
    ? "ws://localhost:8080/ws" 
    : `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/ws`);

