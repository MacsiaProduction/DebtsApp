import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

let mockInitialRoute = '/';

jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom');

  return {
    ...actual,
    BrowserRouter: ({ children }) => (
      <actual.MemoryRouter initialEntries={[mockInitialRoute]}>{children}</actual.MemoryRouter>
    ),
  };
});

jest.mock('./components/NavBar', () => () => <nav data-testid="navbar" />);
jest.mock('./pages/Login', () => () => <div>Login</div>);
jest.mock('./pages/Transactions', () => () => <div>Transactions</div>);
jest.mock('./pages/Debts', () => () => <div>Debts</div>);

describe('App routing', () => {
  beforeEach(() => {
    localStorage.clear();
    mockInitialRoute = '/';
  });

  test('redirects unauthenticated users from root to login', () => {
    render(<App />);
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  test('shows transactions when token is present', () => {
    localStorage.setItem('token', 'jwt');
    mockInitialRoute = '/transactions';

    render(<App />);

    expect(screen.getByText('Transactions')).toBeInTheDocument();
  });
});
