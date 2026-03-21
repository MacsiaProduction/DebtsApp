import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import Debts from './Debts';

jest.mock('../services/api');

const mockedGetDebts = api.getDebts;
const mockedGetDebtBetween = api.getDebtBetween;

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

  test('shows debt between two users in chat', async () => {
    mockedGetDebts.mockResolvedValue([]);
    mockedGetDebtBetween.mockResolvedValue({
      from: 'Creditor',
      to: 'Debtor',
      sum: 500,
      chatId: 10,
    });

    render(
      <MemoryRouter>
        <Debts />
      </MemoryRouter>,
    );

    const chatInput = await screen.findByLabelText(/id чата/i);
    const fromInput = screen.getByLabelText(/кредитор \(имя\)/i);
    const toInput = screen.getByLabelText(/должник \(имя\)/i);

    fireEvent.change(chatInput, { target: { value: '10' } });
    fireEvent.change(fromInput, { target: { value: 'Creditor' } });
    fireEvent.change(toInput, { target: { value: 'Debtor' } });

    const button = screen.getByRole('button', { name: /показать долг/i });
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockedGetDebtBetween).toHaveBeenCalledWith('10', 'Creditor', 'Debtor');
      expect(
        screen.getByText(/долг между creditor и debtor в чате 10/i),
      ).toBeInTheDocument();
      expect(screen.getByText(/500/)).toBeInTheDocument();
    });
  });
});
