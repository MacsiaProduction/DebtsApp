import React from 'react';
import { Navbar, Nav, Button, Container } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';

function NavBar() {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  return (
    <Navbar bg="light" expand="lg">
      <Container>
        <Navbar.Brand as={Link} to="/">
          DebtsApp
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            {token && (
              <>
                <Nav.Link as={Link} to="/transactions">
                  Транзакции
                </Nav.Link>
                <Nav.Link as={Link} to="/debts">
                  Долги
                </Nav.Link>
                <Nav.Link as={Link} to="/new">
                  ➕ Добавить
                </Nav.Link>
                <Nav.Link as={Link} to="/profile">
                  Профиль
                </Nav.Link>
              </>
            )}
          </Nav>
          {token && (
            <Button variant="outline-secondary" onClick={handleLogout}>
              Выйти
            </Button>
          )}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}

export default NavBar;