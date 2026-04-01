import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import NewTransaction from './NewTransaction';

jest.mock('../services/api');

const mockedAddTransaction = api.addTransaction;

describe('NewTransaction page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('submits new transaction form', async () => {
    mockedAddTransaction.mockResolvedValue({});

    render(
      <MemoryRouter>
        <NewTransaction />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText(/кому \(имя получателя\)/i), {
      target: { value: 'UserB' },
    });
    fireEvent.change(screen.getByLabelText(/сумма/i), { target: { value: '150' } });
    fireEvent.change(screen.getByLabelText(/комментарий/i), {
      target: { value: 'For dinner' },
    });

    fireEvent.click(screen.getByRole('button', { name: /создать/i }));

    await waitFor(() => {
      expect(mockedAddTransaction).toHaveBeenCalledWith({
        toName: 'UserB',
        sum: '150',
        comment: 'For dinner',
      });
      expect(
        screen.getByText(/транзакция добавлена! перенаправление/i),
      ).toBeInTheDocument();
    });
  });
});
