import React, { useEffect, useState } from 'react';
import { Container, Table, Spinner, Alert, Form, Row, Col, Button, Card } from 'react-bootstrap';
import {
  addTransaction,
  deleteTransaction,
  getTransactions,
  getTransactionsBetween,
  updateTransactionComment,
} from '../services/api';

function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [sender, setSender] = useState('');
  const [recipient, setRecipient] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editingComment, setEditingComment] = useState('');
  const [submittingAction, setSubmittingAction] = useState(false);
  const [newTransactionForm, setNewTransactionForm] = useState({
    toName: '',
    sum: '',
    comment: '',
  });
  const [createSuccess, setCreateSuccess] = useState('');
  const currentUsername = localStorage.getItem('username');

  const loadData = async (nextSender = '', nextRecipient = '') => {
    setLoading(true);
    setError('');
    try {
      const hasPairFilter = nextSender.trim() && nextRecipient.trim();
      const data = hasPairFilter
        ? await getTransactionsBetween(nextSender.trim(), nextRecipient.trim())
        : await getTransactions();
      setTransactions(data);
    } catch (err) {
      const message = err.message || '';
      if (/not found|не найден/i.test(message)) {
        setTransactions([]);
      } else {
        setError(message);
        setTransactions([]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setActionMessage('');
    setCreateSuccess('');
    const hasSender = sender.trim().length > 0;
    const hasRecipient = recipient.trim().length > 0;

    if (hasSender !== hasRecipient) {
      setError('Для фильтра укажите обоих пользователей.');
      return;
    }

    await loadData(sender, recipient);
  };

  const handleFilterReset = async () => {
    setSender('');
    setRecipient('');
    setActionMessage('');
    setCreateSuccess('');
    await loadData();
  };

  const handleNewTransactionChange = (e) => {
    setNewTransactionForm((currentForm) => ({
      ...currentForm,
      [e.target.name]: e.target.value,
    }));
  };

  const handleCreateTransaction = async (e) => {
    e.preventDefault();
    setSubmittingAction(true);
    setError('');
    setActionMessage('');
    setCreateSuccess('');

    try {
      await addTransaction(newTransactionForm);
      setCreateSuccess('Транзакция добавлена.');
      setNewTransactionForm({
        toName: '',
        sum: '',
        comment: '',
      });
      await loadData(sender, recipient);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmittingAction(false);
    }
  };

  const startEditing = (tx) => {
    setEditingId(tx.id);
    setEditingComment(tx.comment || '');
    setError('');
    setActionMessage('');
    setCreateSuccess('');
  };

  const cancelEditing = () => {
    setEditingId(null);
    setEditingComment('');
  };

  const handleSaveComment = async (transactionId) => {
    setSubmittingAction(true);
    setError('');
    setActionMessage('');
    setCreateSuccess('');
    try {
      await updateTransactionComment(transactionId, editingComment);
      setTransactions((currentTransactions) =>
        currentTransactions.map((tx) =>
          tx.id === transactionId ? { ...tx, comment: editingComment } : tx,
        ),
      );
      setActionMessage('Комментарий обновлён.');
      cancelEditing();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleDeleteTransaction = async (transactionId) => {
    setSubmittingAction(true);
    setError('');
    setActionMessage('');
    setCreateSuccess('');
    try {
      const deleted = await deleteTransaction(transactionId);
      cancelEditing();
      await loadData(sender, recipient);
      setActionMessage(
        deleted?.comment
          ? `Транзакция удалена: ${deleted.comment}`
          : 'Транзакция удалена.',
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmittingAction(false);
    }
  };

  if (loading) {
    return (
      <Container className="text-center mt-5">
        <Spinner animation="border" />
      </Container>
    );
  }

  return (
    <Container className="mt-4">
      <h2>Транзакции</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      {actionMessage && <Alert variant="success">{actionMessage}</Alert>}
      {createSuccess && <Alert variant="success">{createSuccess}</Alert>}
      <Card className="mb-4">
        <Card.Body>
          <Card.Title>Новая транзакция</Card.Title>
          <Form onSubmit={handleCreateTransaction}>
            <Row className="g-3">
              <Col xs={12} md={4}>
                <Form.Label htmlFor="newToName">Кому (имя получателя)</Form.Label>
                <Form.Control
                  id="newToName"
                  type="text"
                  name="toName"
                  value={newTransactionForm.toName}
                  onChange={handleNewTransactionChange}
                  required
                />
              </Col>
              <Col xs={12} md={3}>
                <Form.Label htmlFor="newSum">Сумма</Form.Label>
                <Form.Control
                  id="newSum"
                  type="number"
                  name="sum"
                  value={newTransactionForm.sum}
                  onChange={handleNewTransactionChange}
                  required
                  min="1"
                />
              </Col>
              <Col xs={12} md={5}>
                <Form.Label htmlFor="newComment">Комментарий</Form.Label>
                <Form.Control
                  id="newComment"
                  as="textarea"
                  rows={1}
                  name="comment"
                  value={newTransactionForm.comment}
                  onChange={handleNewTransactionChange}
                />
              </Col>
              <Col xs="auto">
                <Button type="submit" disabled={submittingAction}>
                  Создать транзакцию
                </Button>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>
      <Form className="mb-4" onSubmit={handleFilterSubmit}>
        <Row className="align-items-end g-2">
          <Col xs={12} md={4}>
            <Form.Label htmlFor="txSender">Отправитель (имя)</Form.Label>
            <Form.Control
              id="txSender"
              type="text"
              value={sender}
              onChange={(e) => setSender(e.target.value)}
            />
          </Col>
          <Col xs={12} md={4}>
            <Form.Label htmlFor="txRecipient">Получатель (имя)</Form.Label>
            <Form.Control
              id="txRecipient"
              type="text"
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
            />
          </Col>
          <Col xs="auto" className="mt-3">
            <Button variant="primary" type="submit">
              Найти
            </Button>
          </Col>
          <Col xs="auto" className="mt-3">
            <Button variant="outline-secondary" type="button" onClick={handleFilterReset}>
              Сбросить
            </Button>
          </Col>
        </Row>
      </Form>
      {!transactions.length && !loading && !error && !createSuccess && (
        <Alert variant="info">Нет транзакций для отображения.</Alert>
      )}
      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>Отправитель</th>
            <th>Получатель</th>
            <th>Сумма</th>
            <th>Комментарий</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx, index) => (
            <tr key={tx.id || index}>
              <td>{tx.sender}</td>
              <td>{tx.recipient}</td>
              <td>{`${tx.sum} ₽`}</td>
              <td>
                {editingId === tx.id ? (
                  <Form.Control
                    aria-label={`Комментарий транзакции ${tx.id || index}`}
                    as="textarea"
                    rows={2}
                    maxLength={50}
                    value={editingComment}
                    onChange={(e) => setEditingComment(e.target.value)}
                  />
                ) : (
                  tx.comment
                )}
              </td>
              <td>
                {editingId === tx.id ? (
                  <>
                    <Button
                      className="me-2"
                      size="sm"
                      onClick={() => handleSaveComment(tx.id)}
                      disabled={submittingAction}
                    >
                      Сохранить
                    </Button>
                    <Button
                      variant="outline-secondary"
                      size="sm"
                      onClick={cancelEditing}
                      disabled={submittingAction}
                    >
                      Отмена
                    </Button>
                  </>
                ) : (
                  <>
                    <Button
                      className="me-2"
                      variant="outline-primary"
                      size="sm"
                      onClick={() => startEditing(tx)}
                      disabled={submittingAction || (currentUsername && tx.sender !== currentUsername)}
                    >
                      Изменить комментарий
                    </Button>
                    <Button
                      variant="outline-danger"
                      size="sm"
                      onClick={() => handleDeleteTransaction(tx.id)}
                      disabled={submittingAction || (currentUsername && tx.sender !== currentUsername)}
                    >
                      Удалить
                    </Button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}

export default Transactions;
