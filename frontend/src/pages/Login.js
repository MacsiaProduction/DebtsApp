import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register, getSessionToken, loginBySessionToken } from '../services/api';
import { Form, Button, Container, Alert, Tabs, Tab, Row, Col, InputGroup } from 'react-bootstrap';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [sessionToken, setSessionToken] = useState('');
  const [generatedSessionToken, setGeneratedSessionToken] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      const data = await login(username, password);
      localStorage.setItem('token', data.token);
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
    } catch (err) {
      setError(err.message);
    }
  };

  const handleGenerateSession = async () => {
    setError('');
    setSuccess('');
    try {
      const token = await getSessionToken();
      setGeneratedSessionToken(token);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleLoginBySession = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      const data = await loginBySessionToken(sessionToken);
      localStorage.setItem('token', data.token);
      navigate('/transactions');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Container className="mt-5" style={{ maxWidth: '600px' }}>
      <h2 className="mb-4">Вход и регистрация</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}
      <Tabs defaultActiveKey="login" className="mb-3">
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
        <Tab eventKey="session" title="Вход по сессионному токену">
          <Row className="mt-3">
            <Col md={12}>
              <p className="small text-muted">
                Сессионный токен используется для входа через Telegram-бота. Здесь вы можете
                сгенерировать новый токен и войти по уже полученному.
              </p>
            </Col>
          </Row>
          <Row className="g-2 align-items-end mb-3">
            <Col md={8}>
              <Form.Label>Сгенерированный одноразовый токен</Form.Label>
              <InputGroup>
                <Form.Control
                  type="text"
                  value={generatedSessionToken}
                  readOnly
                  placeholder="Нажмите &quot;Сгенерировать&quot;"
                />
              </InputGroup>
            </Col>
            <Col md={4}>
              <Button variant="outline-primary" onClick={handleGenerateSession}>
                Сгенерировать
              </Button>
            </Col>
          </Row>
          <Form onSubmit={handleLoginBySession}>
            <Form.Group className="mb-3" controlId="sessionToken">
              <Form.Label>Сессионный токен</Form.Label>
              <Form.Control
                type="text"
                value={sessionToken}
                onChange={(e) => setSessionToken(e.target.value)}
                required
              />
            </Form.Group>
            <Button variant="primary" type="submit">
              Войти по токену
            </Button>
          </Form>
        </Tab>
      </Tabs>
    </Container>
  );
}

export default Login;