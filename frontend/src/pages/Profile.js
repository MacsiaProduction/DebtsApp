import React, { useState } from 'react';
import { Container, Card, Button, Alert, Form } from 'react-bootstrap';
import { getLinkToken } from '../services/api';

function Profile() {
  const [linkToken, setLinkToken] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleGetLinkToken = async () => {
    setError('');
    setSuccess('');
    try {
      const token = await getLinkToken();
      setLinkToken(token);
      setSuccess('Токен привязки успешно получен. Используйте его в Telegram-боте для связки аккаунта.');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Container className="mt-4" style={{ maxWidth: '700px' }}>
      <h2 className="mb-4">Профиль</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}
      <Card>
        <Card.Body>
          <Card.Title>Привязка Telegram-аккаунта</Card.Title>
          <Card.Text>
            Получите токен привязки и отправьте его в Telegram-боте, чтобы связать веб-аккаунт с Telegram.
          </Card.Text>
          <Button variant="primary" onClick={handleGetLinkToken}>
            Получить токен привязки
          </Button>
          <Form.Group className="mt-3">
            <Form.Label>Токен привязки</Form.Label>
            <Form.Control
              type="text"
              value={linkToken}
              readOnly
              placeholder="Токен появится здесь после запроса"
            />
          </Form.Group>
        </Card.Body>
      </Card>
    </Container>
  );
}

export default Profile;

