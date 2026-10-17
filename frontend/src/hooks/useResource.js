import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../services/api';

// All page reads share cancellation, retry, and optional background refresh.
// Requests are aborted on navigation and before a newer read replaces them.
export default function useResource(load, intervalMs = 0) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const request = useRef(null);

  const refresh = useCallback(async () => {
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    setRefreshing(true);
    setError('');
    try {
      const result = await load({ signal: controller.signal });
      if (!controller.signal.aborted) setData(result);
      return result;
    } catch (requestError) {
      if (!controller.signal.aborted) setError(errorMessage(requestError));
      return null;
    } finally {
      if (!controller.signal.aborted) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, [load]);

  const replaceData = useCallback((result) => {
    request.current?.abort();
    setData(result);
    setLoading(false);
    setRefreshing(false);
    setError('');
  }, []);

  useEffect(() => {
    refresh();
    const interval = intervalMs ? window.setInterval(() => {
      if (document.visibilityState === 'visible') refresh();
    }, intervalMs) : null;
    return () => {
      request.current?.abort();
      if (interval) window.clearInterval(interval);
    };
  }, [refresh, intervalMs]);

  return { data, replaceData, loading, refreshing, error, refresh };
}
