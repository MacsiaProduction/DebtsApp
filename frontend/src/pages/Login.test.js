import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import Login from './Login';

jest.mock('../services/api', () => {
  const actual = jest.requireActual('../services/api');

  return {
    ...actual,
    login: jest.fn(),
    register: jest.fn(),
  };
});

const mockedLogin = api.login;
const mockedRegister = api.register;

describe('Login page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('renders password login form and logs in user', async () => {
    mockedLogin.mockResolvedValue({ token: 'jwt-token' });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    const loginPanel = screen.getAllByRole('tabpanel')[0];
    const usernameInput = within(loginPanel).getByLabelText(/имя пользователя/i);
    const passwordInput = within(loginPanel).getByLabelText(/пароль/i);
    const submitButton = within(loginPanel).getByRole('button', { name: /войти/i });

    fireEvent.change(usernameInput, { target: { value: 'user1' } });
    fireEvent.change(passwordInput, { target: { value: 'pass1' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockedLogin).toHaveBeenCalledWith('user1', 'pass1');
      expect(localStorage.getItem('token')).toBe('jwt-token');
    });
  });

  test('shows error on login failure', async () => {
    mockedLogin.mockRejectedValue(new Error('Ошибка авторизации'));

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    const loginPanel = screen.getAllByRole('tabpanel')[0];
    const usernameInput = within(loginPanel).getByLabelText(/имя пользователя/i);
    const passwordInput = within(loginPanel).getByLabelText(/пароль/i);
    const submitButton = within(loginPanel).getByRole('button', { name: /войти/i });

    fireEvent.change(usernameInput, { target: { value: 'user1' } });
    fireEvent.change(passwordInput, { target: { value: 'wrong' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/ошибка авторизации/i)).toBeInTheDocument();
    });
  });

  test('registers new user', async () => {
    mockedRegister.mockResolvedValue({});

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    const registerTab = screen.getByRole('tab', { name: /регистрация/i });
    fireEvent.click(registerTab);

    const registerPanel = screen.getAllByRole('tabpanel')[1];
    const usernameInput = within(registerPanel).getByLabelText(/имя пользователя/i);
    const passwordInput = within(registerPanel).getByLabelText(/пароль/i);
    const submitButton = within(registerPanel).getByRole('button', { name: /зарегистрироваться/i });

    fireEvent.change(usernameInput, { target: { value: 'newuser' } });
    fireEvent.change(passwordInput, { target: { value: 'newpass' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockedRegister).toHaveBeenCalledWith('newuser', 'newpass');
      expect(screen.getByText(/регистрация прошла успешно/i)).toBeInTheDocument();
    });
  });

  test('redirects authenticated user away from login page', () => {
    localStorage.setItem('token', 'jwt-by-session');

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Login />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('button', { name: /войти/i })).not.toBeInTheDocument();
  });
});
