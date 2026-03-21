import React, { useEffect, useState } from 'react';
import { Container, Table, Spinner, Alert, Form, Row, Col, Button } from 'react-bootstrap';
import {
  getTransactions,
  getTransactionsInChat,
  getTransactionsBetween,
  getTransactionsBetweenInChat,
} from '../services/api';

function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [mode, setMode] = useState('all');
  const [chatId, setChatId] = useState('');
  const [sender, setSender] = useState('');
  const [recipient, setRecipient] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      let data = [];
      if (mode === 'all') {
        data = await getTransactions();
      } else if (mode === 'chat') {
        data = await getTransactionsInChat(chatId);
      } else if (mode === 'between') {
        data = await getTransactionsBetween(sender, recipient);
      } else if (mode === 'betweenChat') {
        data = await getTransactionsBetweenInChat(chatId, sender, recipient);
      }
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

  if (loading) {
    return (
      <Container className="text-center mt-5">
        <Spinner animation="border" />
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="mt-5">
        <Alert variant="danger">{error}</Alert>
      </Container>
    );
  }

  return (
    <Container className="mt-4">
      <h2>Все транзакции</h2>
      <Form className="mb-4">
        <Row className="align-items-end g-2">
          <Col xs={12} md={3}>
            <Form.Label>Режим просмотра</Form.Label>
            <Form.Select
              value={mode}
              onChange={(e) => setMode(e.target.value)}
            >
              <option value="all">Все мои транзакции</option>
              <option value="chat">Мои транзакции в чате</option>
              <option value="between">Между двумя пользователями</option>
              <option value="betweenChat">Между двумя в чате</option>
            </Form.Select>
          </Col>
          {(mode === 'chat' || mode === 'betweenChat') && (
            <Col xs={12} md={3}>
              <Form.Label>ID чата</Form.Label>
              <Form.Control
                type="number"
                value={chatId}
                onChange={(e) => setChatId(e.target.value)}
              />
            </Col>
          )}
          {(mode === 'between' || mode === 'betweenChat') && (
            <>
              <Col xs={12} md={3}>
                <Form.Label>Отправитель (имя)</Form.Label>
                <Form.Control
                  type="text"
                  value={sender}
                  onChange={(e) => setSender(e.target.value)}
                />
              </Col>
              <Col xs={12} md={3}>
                <Form.Label>Получатель (имя)</Form.Label>
                <Form.Control
                  type="text"
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                />
              </Col>
            </>
          )}
          <Col xs="auto" className="mt-3">
            <Button variant="primary" onClick={loadData}>
              Применить фильтр
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
            <th>ID чата</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx, index) => (
            <tr key={index}>
              <td>{tx.sender}</td>
              <td>{tx.recipient}</td>
              <td>{tx.sum}</td>
              <td>{tx.comment}</td>
              <td>{tx.chatId}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}

export default Transactions;