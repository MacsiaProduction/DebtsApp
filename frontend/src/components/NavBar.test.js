import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NavBar from './NavBar';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('NavBar', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockReset();
  });

  test('renders links when user is authenticated', () => {
    localStorage.setItem('token', 'jwt');

    render(
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>,
    );

    expect(screen.getByText(/DebtsApp/i)).toBeInTheDocument();
    expect(screen.getByText(/Транзакции/i)).toBeInTheDocument();
    expect(screen.getByText(/Долги/i)).toBeInTheDocument();
    expect(screen.getByText(/Профиль/i)).toBeInTheDocument();
  });

  test('logs out user on button click', () => {
    localStorage.setItem('token', 'jwt');

    render(
      <MemoryRouter>
        <NavBar />
      </MemoryRouter>,
    );

    const button = screen.getByRole('button', { name: /выйти/i });
    fireEvent.click(button);

    expect(localStorage.getItem('token')).toBeNull();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
}

