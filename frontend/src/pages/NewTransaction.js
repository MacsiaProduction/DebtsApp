import React, { useState } from 'react';
import { Container, Form, Button, Alert } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { addTransaction } from '../services/api';

function NewTransaction() {
  const [form, setForm] = useState({
    chatId: '',
    toName: '',
    sum: '',
    comment: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await addTransaction(form);
      setSuccess(true);
      setTimeout(() => navigate('/transactions'), 1500);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Container className="mt-4" style={{ maxWidth: '500px' }}>
      <h2>Новая транзакция</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">Транзакция добавлена! Перенаправление...</Alert>}
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3" controlId="chatId">
          <Form.Label>ID чата</Form.Label>
          <Form.Control
            type="number"
            name="chatId"
            value={form.chatId}
            onChange={handleChange}
            required
          />
        </Form.Group>
        <Form.Group className="mb-3" controlId="toName">
          <Form.Label>Кому (имя получателя)</Form.Label>
          <Form.Control
            type="text"
            name="toName"
            value={form.toName}
            onChange={handleChange}
            required
          />
        </Form.Group>
        <Form.Group className="mb-3" controlId="sum">
          <Form.Label>Сумма</Form.Label>
          <Form.Control
            type="number"
            name="sum"
            value={form.sum}
            onChange={handleChange}
            required
            min="1"
          />
        </Form.Group>
        <Form.Group className="mb-3" controlId="comment">
          <Form.Label>Комментарий</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            name="comment"
            value={form.comment}
            onChange={handleChange}
          />
        </Form.Group>
        <Button variant="primary" type="submit">
          Создать
        </Button>
      </Form>
    </Container>
  );
}

export default NewTransaction;