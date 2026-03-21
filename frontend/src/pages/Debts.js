import React, { useEffect, useState } from 'react';
import { Container, Table, Spinner, Alert, Form, Row, Col, Button, Card } from 'react-bootstrap';
import { getDebts, getDebtBetween } from '../services/api';

function Debts() {
  const [debts, setDebts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [chatId, setChatId] = useState('');
  const [fromName, setFromName] = useState('');
  const [toName, setToName] = useState('');
  const [betweenResult, setBetweenResult] = useState(null);
  const [betweenError, setBetweenError] = useState('');

  useEffect(() => {
    const fetchDebts = async () => {
      try {
        const data = await getDebts();
        setDebts(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchDebts();
  }, []);

  const handleBetweenSubmit = async (e) => {
    e.preventDefault();
    setBetweenError('');
    setBetweenResult(null);
    try {
      const result = await getDebtBetween(chatId, fromName, toName);
      setBetweenResult(result);
    } catch (err) {
      setBetweenError(err.message);
    }
  };

  if (loading) {
    return (
      <Container className="text-center mt-5">
        <Spinner animation="border" variant="primary" />
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
      <h2>Все долги</h2>
      {!debts.length && !loading && !error && (
        <Alert variant="info">Нет долгов для отображения.</Alert>
      )}
      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>От кого</th>
            <th>Кому</th>
            <th>Сумма</th>
            <th>ID чата</th>
          </tr>
        </thead>
        <tbody>
          {debts.map((debt, index) => (
            <tr key={index}>
              <td>{debt.from}</td>
              <td>{debt.to}</td>
              <td>{debt.sum}</td>
              <td>{debt.chatId}</td>
            </tr>
          ))}
        </tbody>
      </Table>
      <Card className="mt-4">
        <Card.Body>
          <Card.Title>Посмотреть долг между двумя пользователями в чате</Card.Title>
          <Form onSubmit={handleBetweenSubmit}>
            <Row className="g-2">
              <Col xs={12} md={3}>
                <Form.Label htmlFor="betweenChatId">ID чата</Form.Label>
                <Form.Control
                  id="betweenChatId"
                  type="number"
                  value={chatId}
                  onChange={(e) => setChatId(e.target.value)}
                  required
                />
              </Col>
              <Col xs={12} md={4}>
                <Form.Label htmlFor="betweenFromName">Кредитор (имя)</Form.Label>
                <Form.Control
                  id="betweenFromName"
                  type="text"
                  value={fromName}
                  onChange={(e) => setFromName(e.target.value)}
                  required
                />
              </Col>
              <Col xs={12} md={4}>
                <Form.Label htmlFor="betweenToName">Должник (имя)</Form.Label>
                <Form.Control
                  id="betweenToName"
                  type="text"
                  value={toName}
                  onChange={(e) => setToName(e.target.value)}
                  required
                />
              </Col>
              <Col xs="auto" className="mt-3">
                <Button type="submit" variant="primary">
                  Показать долг
                </Button>
              </Col>
            </Row>
          </Form>
          {betweenError && (
            <Alert className="mt-3" variant="danger">
              {betweenError}
            </Alert>
          )}
          {betweenResult && (
            <Alert className="mt-3" variant="info">
              Долг между {betweenResult.from} и {betweenResult.to} в чате {betweenResult.chatId}:{' '}
              <strong>{betweenResult.sum}</strong>
            </Alert>
          )}
        </Card.Body>
      </Card>
    </Container>
  );
}

export default Debts;