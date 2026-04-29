import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

describe('App routing', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState({}, '', '/');
  });

  test('renders the protected new transaction route for authenticated users', () => {
    localStorage.setItem('token', 'jwt');
    window.history.pushState({}, '', '/new');

    render(<App />);

    expect(screen.getByRole('heading', { name: /новая транзакция/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /создать/i })).toBeInTheDocument();
  });
});
