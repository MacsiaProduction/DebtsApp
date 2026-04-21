import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import Transactions from './Transactions';

jest.mock('../services/api');

const mockedAddTransaction = api.addTransaction;
const mockedGetTransactions = api.getTransactions;
const mockedGetTransactionsBetween = api.getTransactionsBetween;
const mockedUpdateTransactionComment = api.updateTransactionComment;
const mockedDeleteLastTransaction = api.deleteLastTransaction;

describe('Transactions page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('creates transaction from transactions page', async () => {
    mockedGetTransactions.mockResolvedValue([]);
    mockedAddTransaction.mockResolvedValue({});

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    fireEvent.change(await screen.findByLabelText(/кому \(имя получателя\)/i), {
      target: { value: 'UserB' },
    });
    fireEvent.change(screen.getByLabelText(/^сумма$/i), { target: { value: '150' } });
    fireEvent.change(screen.getByLabelText(/^комментарий$/i), {
      target: { value: 'For dinner' },
    });

    fireEvent.click(screen.getByRole('button', { name: /создать транзакцию/i }));

    await waitFor(() => {
      expect(mockedAddTransaction).toHaveBeenCalledWith({
        toName: 'UserB',
        sum: '150',
        comment: 'For dinner',
      });
      expect(screen.getByText(/транзакция добавлена/i)).toBeInTheDocument();
    });
  });

  test('loads and displays transactions list', async () => {
    mockedGetTransactions.mockResolvedValue([
      { id: 1, sender: 'A', recipient: 'B', sum: 100, comment: 'test', chatId: 1 },
    ]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/транзакции/i)).toBeInTheDocument();
      expect(screen.getByText('A')).toBeInTheDocument();
      expect(screen.getByText('B')).toBeInTheDocument();
      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText('test')).toBeInTheDocument();
    });
  });

  test('applies between users filter', async () => {
    mockedGetTransactions.mockResolvedValue([]);
    mockedGetTransactionsBetween.mockResolvedValue([
      { id: 2, sender: 'User1', recipient: 'User2', sum: 10, comment: '', chatId: 5 },
    ]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    const senderInput = await screen.findByLabelText(/отправитель \(имя\)/i);
    const recipientInput = screen.getByLabelText(/получатель \(имя\)/i);

    fireEvent.change(senderInput, { target: { value: 'User1' } });
    fireEvent.change(recipientInput, { target: { value: 'User2' } });

    const applyButton = screen.getByRole('button', { name: /найти/i });
    fireEvent.click(applyButton);

    await waitFor(() => {
      expect(mockedGetTransactionsBetween).toHaveBeenCalledWith('User1', 'User2');
      expect(screen.getByText('User1')).toBeInTheDocument();
      expect(screen.getByText('User2')).toBeInTheDocument();
    });
  });

  test('shows validation error when only one participant is set', async () => {
    mockedGetTransactions.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    const senderInput = await screen.findByLabelText(/отправитель \(имя\)/i);
    fireEvent.change(senderInput, { target: { value: 'User1' } });

    fireEvent.click(screen.getByRole('button', { name: /найти/i }));

    await waitFor(() => {
      expect(screen.getByText(/для фильтра укажите обоих пользователей/i)).toBeInTheDocument();
    });
    expect(mockedGetTransactionsBetween).not.toHaveBeenCalled();
  });

  test('updates transaction comment inline', async () => {
    localStorage.setItem('username', 'User1');
    mockedGetTransactions.mockResolvedValue([
      { id: 42, sender: 'User1', recipient: 'User2', sum: 15, comment: 'old', chatId: 5 },
    ]);
    mockedUpdateTransactionComment.mockResolvedValue({});

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole('button', { name: /изменить комментарий/i }));
    fireEvent.change(screen.getByLabelText(/комментарий транзакции 42/i), {
      target: { value: 'new comment' },
    });
    fireEvent.click(screen.getByRole('button', { name: /сохранить/i }));

    await waitFor(() => {
      expect(mockedUpdateTransactionComment).toHaveBeenCalledWith(42, 'new comment');
      expect(screen.getByText('new comment')).toBeInTheDocument();
    });
  });

  test('deletes last transaction and reloads list', async () => {
    mockedGetTransactions
      .mockResolvedValueOnce([{ id: 1, sender: 'A', recipient: 'B', sum: 100, comment: 'first', chatId: 1 }])
      .mockResolvedValueOnce([]);
    mockedDeleteLastTransaction.mockResolvedValue({ comment: 'first' });

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole('button', { name: /удалить последнюю мою/i }));

    await waitFor(() => {
      expect(mockedDeleteLastTransaction).toHaveBeenCalled();
      expect(mockedGetTransactions).toHaveBeenCalledTimes(2);
      expect(screen.getByText(/последняя транзакция удалена/i)).toBeInTheDocument();
    });
  });
});
