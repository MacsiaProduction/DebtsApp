import React, { useEffect, useState } from 'react';
import { Container, Table, Spinner, Alert, Form, Row, Col, Button } from 'react-bootstrap';
import { getTransactions, getTransactionsBetween } from '../services/api';

function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sender, setSender] = useState('');
  const [recipient, setRecipient] = useState('');

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
      setError(err.message);
      setTransactions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const hasSender = sender.trim().length > 0;
    const hasRecipient = recipient.trim().length > 0;

    if (hasSender !== hasRecipient) {
      setError('Для фильтра укажите обоих пользователей.');
      return;
    }

    await loadData(sender, recipient);
  };

  const handleReset = async () => {
    setSender('');
    setRecipient('');
    await loadData();
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
      <Form className="mb-4" onSubmit={handleSubmit}>
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
            <Button variant="outline-secondary" type="button" onClick={handleReset}>
              Сбросить
            </Button>
          </Col>
        </Row>
      </Form>
      {!transactions.length && !loading && !error && (
        <Alert variant="info">Нет транзакций для отображения.</Alert>
      )}
      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>Отправитель</th>
            <th>Получатель</th>
            <th>Сумма</th>
            <th>Комментарий</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx, index) => (
            <tr key={index}>
              <td>{tx.sender}</td>
              <td>{tx.recipient}</td>
              <td>{tx.sum}</td>
              <td>{tx.comment}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}

export default Transactions;