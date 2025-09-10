import React, { useState, useEffect } from 'react';
import Login from './Login';
import Dashboard from './Dashboard';

// Types
interface User {
  username: string;
  role: string;
}

const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Check if user is already logged in on app start
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser(userData);
      } catch (error) {
        console.error('Error parsing saved user data:', error);
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  // Check API health on start
  useEffect(() => {
    const checkAPIHealth = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/public/health');
        if (response.ok) {
          const data = await response.json();
          console.log('API Status:', data.status);
        }
      } catch (error) {
        console.error('API health check failed:', error);
      }
    };

    checkAPIHealth();
  }, []);

  const handleLoginSuccess = (userData: User) => {
    setUser(userData);
  };

  const handleLogout = () => {
    setUser(null);
  };

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        background: '#C4E7E0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <div style={{ fontSize: '18px', color: '#374151' }}>
          กำลังโหลด...
        </div>
      </div>
    );
  }

  // Show Dashboard if user is logged in, otherwise show Login
  return user ? (
    <Dashboard user={user} onLogout={handleLogout} />
  ) : (
    <Login onLoginSuccess={handleLoginSuccess} />
  );
};

export default App;