import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { hasStoredToken, login, register } from '../services/api';
import { Form, Button, Container, Alert, Tabs, Tab } from 'react-bootstrap';

function Login() {
  const [activeTab, setActiveTab] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  if (hasStoredToken()) {
    return <Navigate to="/transactions" replace />;
  }

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      const data = await login(username, password);
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', username);
      navigate('/transactions');
    } catch (err) {
      setError(err.message);
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await register(regUsername, regPassword);
      setSuccess('Регистрация прошла успешно. Теперь можете войти.');
      setUsername(regUsername);
      setPassword(regPassword);
      setActiveTab('login');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Container className="mt-5" style={{ maxWidth: '600px' }}>
      <h2 className="mb-4">DebtsApp</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}
      <Tabs activeKey={activeTab} onSelect={(key) => setActiveTab(key || 'login')} className="mb-3">
        <Tab eventKey="login" title="Вход по паролю">
          <Form onSubmit={handleLoginSubmit}>
            <Form.Group className="mb-3" controlId="loginUsername">
              <Form.Label>Имя пользователя</Form.Label>
              <Form.Control
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="loginPassword">
              <Form.Label>Пароль</Form.Label>
              <Form.Control
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </Form.Group>
            <Button variant="primary" type="submit">
              Войти
            </Button>
          </Form>
        </Tab>
        <Tab eventKey="register" title="Регистрация">
          <Form onSubmit={handleRegisterSubmit}>
            <Form.Group className="mb-3" controlId="regUsername">
              <Form.Label>Имя пользователя</Form.Label>
              <Form.Control
                type="text"
                value={regUsername}
                onChange={(e) => setRegUsername(e.target.value)}
                required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="regPassword">
              <Form.Label>Пароль</Form.Label>
              <Form.Control
                type="password"
                value={regPassword}
                onChange={(e) => setRegPassword(e.target.value)}
                required
              />
            </Form.Group>
            <Button variant="success" type="submit">
              Зарегистрироваться
            </Button>
          </Form>
        </Tab>
      </Tabs>
    </Container>
  );
}

export default Login;
