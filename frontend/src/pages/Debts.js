import React, { useEffect, useState } from 'react';
import { Container, Table, Spinner, Alert } from 'react-bootstrap';
import { getDebts } from '../services/api';

function Debts() {
  const [debts, setDebts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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
      <h2>Долги</h2>
      {!debts.length && !loading && !error && (
        <Alert variant="info">Нет долгов для отображения.</Alert>
      )}
      <Table striped bordered hover responsive>
        <thead>
          <tr>
            <th>От кого</th>
            <th>Кому</th>
            <th>Сумма</th>
          </tr>
        </thead>
        <tbody>
          {debts.map((debt, index) => (
            <tr key={index}>
              <td>{debt.from}</td>
              <td>{debt.to}</td>
              <td>{debt.sum}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Container>
  );
}

export default Debts;