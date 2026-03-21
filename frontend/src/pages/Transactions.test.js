import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../services/api';
import Transactions from './Transactions';

jest.mock('../services/api');

const mockedGetTransactions = api.getTransactions;
const mockedGetTransactionsInChat = api.getTransactionsInChat;
const mockedGetTransactionsBetween = api.getTransactionsBetween;
const mockedGetTransactionsBetweenInChat = api.getTransactionsBetweenInChat;

describe('Transactions page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('loads and displays transactions list', async () => {
    mockedGetTransactions.mockResolvedValue([
      { sender: 'A', recipient: 'B', sum: 100, comment: 'test', chatId: 1 },
    ]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText(/все транзакции/i)).toBeInTheDocument();
      expect(screen.getByText('A')).toBeInTheDocument();
      expect(screen.getByText('B')).toBeInTheDocument();
      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText('test')).toBeInTheDocument();
    });
  });

  test('applies chat filter', async () => {
    mockedGetTransactions.mockResolvedValue([]);
    mockedGetTransactionsInChat.mockResolvedValue([
      { sender: 'ChatUser', recipient: 'Other', sum: 50, comment: '', chatId: 42 },
    ]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    const modeSelect = await screen.findByLabelText(/режим просмотра/i);
    fireEvent.change(modeSelect, { target: { value: 'chat' } });

    const chatInput = screen.getByLabelText(/id чата/i);
    fireEvent.change(chatInput, { target: { value: '42' } });

    const applyButton = screen.getByRole('button', { name: /применить фильтр/i });
    fireEvent.click(applyButton);

    await waitFor(() => {
      expect(mockedGetTransactionsInChat).toHaveBeenCalledWith('42');
      expect(screen.getByText('ChatUser')).toBeInTheDocument();
    });
  });

  test('applies between users filter', async () => {
    mockedGetTransactions.mockResolvedValue([]);
    mockedGetTransactionsBetween.mockResolvedValue([
      { sender: 'User1', recipient: 'User2', sum: 10, comment: '', chatId: 5 },
    ]);

    render(
      <MemoryRouter>
        <Transactions />
      </MemoryRouter>,
    );

    const modeSelect = await screen.findByLabelText(/режим просмотра/i);
    fireEvent.change(modeSelect, { target: { value: 'between' } });

    const senderInput = screen.getByLabelText(/отправитель \(имя\)/i);
    const recipientInput = screen.getByLabelText(/получатель \(имя\)/i);

    fireEvent.change(senderInput, { target: { value: 'User1' } });
    fireEvent.change(recipientInput, { target: { value: 'User2' } });

    const applyButton = screen.getByRole('button', { name: /применить фильтр/i });
    fireEvent.click(applyButton);

    await waitFor(() => {
      expect(mockedGetTransactionsBetween).toHaveBeenCalledWith('User1', 'User2');
      expect(screen.getByText('User1')).toBeInTheDocument();
      expect(screen.getByText('User2')).toBeInTheDocument();
    });
  });
});
