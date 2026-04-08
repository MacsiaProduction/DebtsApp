const getDefaultApiBase = () => {
  if (typeof window === 'undefined') {
    return 'http://localhost:8080';
  }

  const { hostname } = window.location;

  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8080';
  }

  return '/api';
};

const API_BASE = process.env.REACT_APP_API_BASE || getDefaultApiBase();

const getToken = () => localStorage.getItem('token');

const getErrorMessage = (data, fallback) =>
  (data && data.message) || (typeof data === 'string' ? data : null) || fallback;

const normalizeCollection = (result) => {
  if (result && Array.isArray(result.content)) {
    return result.content;
  }

  return Array.isArray(result) ? result : [];
};

const parseResponse = async (response) => {
  const text = await response.text();

  try {
    return text ? JSON.parse(text) : null;
  } catch (error) {
    return text;
  }
};

const clearAuthAndRedirect = () => {
  localStorage.removeItem('token');
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
};

async function apiRequest(endpoint, method = 'GET', body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };

  if (auth) {
    const token = getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const options = { method, headers };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE}${endpoint}`, options);
  const data = await parseResponse(response);

  if (response.status === 401) {
    clearAuthAndRedirect();
    return null;
  }

  if (!response.ok) {
    throw new Error(getErrorMessage(data, 'Ошибка запроса'));
  }

  return data;
}

export const login = async (username, password) => {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

  const data = await parseResponse(response);

  if (!response.ok) {
    throw new Error(getErrorMessage(data, 'Ошибка авторизации'));
  }

  return { token: typeof data === 'string' ? data : data?.token };
};

export const register = (username, password) =>
  apiRequest('/auth/register', 'POST', { username, password }, false);

export const getTransactions = async (page = 0, size = 50) =>
  normalizeCollection(await apiRequest(`/transactions?page=${page}&size=${size}`));

export const addTransaction = ({ toName, sum, comment }) =>
  apiRequest(
    `/new?toName=${encodeURIComponent(toName)}&sum=${encodeURIComponent(sum)}&comment=${encodeURIComponent(
      comment || '',
    )}`,
    'POST',
  );

export const getTransactionsBetween = async (sender, recipient, page = 0, size = 50) =>
  normalizeCollection(
    await apiRequest(
      `/transactions/between?sender=${encodeURIComponent(
        sender,
      )}&recipient=${encodeURIComponent(recipient)}&page=${page}&size=${size}`,
    ),
  );

export const getDebts = async (page = 0, size = 50) =>
  normalizeCollection(await apiRequest(`/debts?page=${page}&size=${size}`));
