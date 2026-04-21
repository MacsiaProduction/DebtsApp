import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Transactions from './pages/Transactions';
import Debts from './pages/Debts';
import NavBar from './components/NavBar';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/transactions"
          element={
            <PrivateRoute>
              <Transactions />
            </PrivateRoute>
          }
        />
        <Route
          path="/debts"
          element={
            <PrivateRoute>
              <Debts />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/transactions" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
