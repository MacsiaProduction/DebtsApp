const API_BASE = '';

const getToken = () => localStorage.getItem('token');

async function apiRequest(endpoint, method = 'GET', body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) {
    const token = getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const options = {
    method,
    headers,
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE}${endpoint}`, options);

  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (e) {
    data = text;
  }

  if (!response.ok) {
    const message =
      (data && data.message) || (typeof data === 'string' ? data : null) || 'Ошибка запроса';
    throw new Error(message);
  }

  return data;
}

export const login = async (username, password) => {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || 'Ошибка авторизации');
  }

  return { token: text };
};

export const getTransactions = async (page = 0, size = 50) => {
  const result = await apiRequest(`/transactions?page=${page}&size=${size}`);
  if (result && Array.isArray(result.content)) {
    return result.content;
  }
  return Array.isArray(result) ? result : [];
};

export const addTransaction = ({ chatId, toName, sum, comment }) =>
  apiRequest(
    `/new?chatId=${encodeURIComponent(chatId)}&toName=${encodeURIComponent(
      toName,
    )}&sum=${encodeURIComponent(sum)}&comment=${encodeURIComponent(comment)}`,
    'POST',
  );

export const getDebts = async (page = 0, size = 50) => {
  const result = await apiRequest(`/debts?page=${page}&size=${size}`);
  if (result && Array.isArray(result.content)) {
    return result.content;
  }
  return Array.isArray(result) ? result : [];
};

export const register = (username, password) =>
  apiRequest('/auth/register', 'POST', { username, password }, false);

export const getTransactionsInChat = async (chatId, page = 0, size = 50) => {
  const result = await apiRequest(
    `/transactions/chat?chatId=${encodeURIComponent(chatId)}&page=${page}&size=${size}`,
  );
  if (result && Array.isArray(result.content)) {
    return result.content;
  }
  return Array.isArray(result) ? result : [];
};

export const getTransactionsBetween = async (sender, recipient, page = 0, size = 50) => {
  const result = await apiRequest(
    `/transactions/between?sender=${encodeURIComponent(
      sender,
    )}&recipient=${encodeURIComponent(recipient)}&page=${page}&size=${size}`,
  );
  if (result && Array.isArray(result.content)) {
    return result.content;
  }
  return Array.isArray(result) ? result : [];
};

export const getTransactionsBetweenInChat = async (
  chatId,
  sender,
  recipient,
  page = 0,
  size = 50,
) => {
  const result = await apiRequest(
    `/transactions/between/chat?chatId=${encodeURIComponent(
      chatId,
    )}&sender=${encodeURIComponent(sender)}&recipient=${encodeURIComponent(
      recipient,
    )}&page=${page}&size=${size}`,
  );
  if (result && Array.isArray(result.content)) {
    return result.content;
  }
  return Array.isArray(result) ? result : [];
};

export const getDebtBetween = (chatId, fromName, toName) =>
  apiRequest(
    `/debts/between?chatId=${encodeURIComponent(chatId)}&fromName=${encodeURIComponent(
      fromName,
    )}&toName=${encodeURIComponent(toName)}`,
  );

export const getSessionToken = () => apiRequest('/session', 'GET', null, false);

export const loginBySessionToken = async (sessionToken) => {
  const response = await fetch(`${API_BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
    body: sessionToken,
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || 'Ошибка входа по сессионному токену');
  }

  return { token: text };
};

export const getLinkToken = () => apiRequest('/auth/link-token');

