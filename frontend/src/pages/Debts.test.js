import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import Debts from './Debts';

jest.mock('../services/api');

const mockedGetDebts = api.getDebts;

describe('Debts page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('loads and displays debts list', async () => {
    mockedGetDebts.mockResolvedValue([
      { from: 'A', to: 'B', sum: 200, chatId: 2 },
    ]);

    render(
      <MemoryRouter>
        <Debts />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('A')).toBeInTheDocument();
      expect(screen.getByText('B')).toBeInTheDocument();
      expect(screen.getByText('200')).toBeInTheDocument();
    });
  });

  test('shows empty state when debts list is empty', async () => {
    mockedGetDebts.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <Debts />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/нет долгов для отображения/i)).toBeInTheDocument();
    });
  });
});
