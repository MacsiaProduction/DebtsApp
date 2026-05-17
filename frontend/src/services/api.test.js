import {
  addTransaction,
  clearStoredAuth,
  deleteTransaction,
  getDebts,
  getStoredToken,
  getTransactions,
  getTransactionsBetween,
  hasStoredToken,
  login,
  register,
  updateTransactionComment,
} from './api';

describe('api auth helpers', () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = jest.fn();
  });

  test('getStoredToken returns null when missing', () => {
    expect(getStoredToken()).toBeNull();
    expect(hasStoredToken()).toBe(false);
  });

  test('getStoredToken ignores invalid placeholder values', () => {
    localStorage.setItem('token', 'undefined');
    expect(getStoredToken()).toBeNull();

    localStorage.setItem('token', 'null');
    expect(getStoredToken()).toBeNull();
  });

  test('getStoredToken returns stored jwt', () => {
    localStorage.setItem('token', 'jwt-token');
    expect(getStoredToken()).toBe('jwt-token');
    expect(hasStoredToken()).toBe(true);
  });

  test('clearStoredAuth removes credentials', () => {
    localStorage.setItem('token', 'jwt-token');
    localStorage.setItem('username', 'alice');

    clearStoredAuth();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
    expect(hasStoredToken()).toBe(false);
  });
});

describe('api requests', () => {
  beforeEach(() => {
    localStorage.clear();
    delete window.location;
    window.location = { pathname: '/transactions', href: '/transactions' };
    global.fetch = jest.fn();
  });

  const jsonResponse = (status, body) => ({
    ok: status >= 200 && status < 300,
    status,
    text: async () => (typeof body === 'string' ? body : JSON.stringify(body)),
  });

  test('login stores token from json body', async () => {
    global.fetch.mockResolvedValue(jsonResponse(200, { token: 'jwt-123' }));

    const result = await login('alice', 'secret');

    expect(result).toEqual({ token: 'jwt-123' });
  });

  test('login accepts plain-text token body', async () => {
    global.fetch.mockResolvedValue(jsonResponse(200, 'plain-token'));

    const result = await login('alice', 'secret');

    expect(result).toEqual({ token: 'plain-token' });
  });

  test('login throws when credentials are invalid', async () => {
    global.fetch.mockResolvedValue(jsonResponse(401, { message: 'bad creds' }));

    await expect(login('alice', 'wrong')).rejects.toThrow('bad creds');
  });

  test('register posts credentials without auth header', async () => {
    global.fetch.mockResolvedValue(jsonResponse(200, { ok: true }));

    await register('alice', 'secret');

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/auth/register',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ username: 'alice', password: 'secret' }),
      }),
    );
  });

  test('getTransactions normalizes paginated content', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(
      jsonResponse(200, { content: [{ id: 1 }, { id: 2 }] }),
    );

    const rows = await getTransactions();

    expect(rows).toHaveLength(2);
  });

  test('getDebts normalizes array response', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(200, [{ id: 'd1' }]));

    const rows = await getDebts();

    expect(rows).toEqual([{ id: 'd1' }]);
  });

  test('getTransactionsBetween encodes query params', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(200, []));

    await getTransactionsBetween('a', 'b');

    expect(global.fetch.mock.calls[0][0]).toContain(
      '/api/transactions/between?sender=a&recipient=b',
    );
  });

  test('addTransaction posts query parameters', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(200, { ok: true }));

    await addTransaction({ toName: 'bob', sum: '10', comment: 'lunch' });

    expect(global.fetch.mock.calls[0][0]).toContain('/api/new?');
    expect(global.fetch.mock.calls[0][0]).toContain('toName=bob');
  });

  test('updateTransactionComment posts encoded comment', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(200, { ok: true }));

    await updateTransactionComment(5, 'updated');

    expect(global.fetch.mock.calls[0][0]).toContain('/api/transactions/5/comment');
  });

  test('deleteTransaction sends DELETE', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(200, { ok: true }));

    await deleteTransaction(9);

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/transactions/9',
      expect.objectContaining({ method: 'DELETE' }),
    );
  });

  test('apiRequest redirects on 401', async () => {
    localStorage.setItem('token', 'jwt');
    global.fetch.mockResolvedValue(jsonResponse(401, 'unauthorized'));

    const result = await getDebts();

    expect(result).toEqual([]);
    expect(window.location.href).toBe('/login');
    expect(localStorage.getItem('token')).toBeNull();
  });
});
