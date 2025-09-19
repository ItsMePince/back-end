import React, { useState } from "react";
import "./Login.css";

// Types for API responses
interface LoginResponse {
  success: boolean;
  message: string;
  user?: {
    username: string;
    role: string;
  };
}

interface LoginProps {
  onLoginSuccess?: (user: { username: string; role: string }) => void;
}

const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // สำหรับ cookies/session
        body: JSON.stringify({ username, password }),
      });

      const data: LoginResponse = await response.json();
      
      if (data.success && data.user) {
        // เก็บข้อมูล user ใน localStorage สำหรับใช้ในหน้าอื่น
        localStorage.setItem('user', JSON.stringify(data.user));
        
        // เรียก callback function ถ้ามี
        if (onLoginSuccess) {
          onLoginSuccess(data.user);
        } else {
          // หรือ redirect ไปหน้า home
          window.location.href = '/home';
        }
      } else {
        setError(data.message || 'Login failed');
      }
    } catch (error) {
      console.error('Login error:', error);
      setError('เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ กรุณาลองใหม่อีกครั้ง');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="card">
        <div className="avatar" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="42" height="42" role="img" aria-label="user">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 20a8 8 0 0116 0" />
          </svg>
        </div>

        <h1 className="title">login</h1>

        <form className="form" onSubmit={onSubmit}>
          <label className="label">
            Username
            <input
              type="text"
              className="input"
              placeholder="user หรือ admin"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={loading}
            />
          </label>

          <label className="label">
            Password
            <input
              type="password"
              className="input"
              placeholder="password หรือ admin"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </label>

          {error && (
            <div style={{ 
              color: '#ef4444', 
              fontSize: '14px', 
              textAlign: 'center',
              margin: '8px 0'
            }}>
              {error}
            </div>
          )}

          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'กำลังเข้าสู่ระบบ...' : 'login'}
          </button>
        </form>

        <div style={{ 
          fontSize: '12px', 
          color: '#6b7280', 
          marginTop: '12px',
          textAlign: 'center'
        }}>
          <div>ทดสอบ: user / password</div>
          <div>แอดมิน: admin / admin</div>
        </div>

        <p className="footnote">
          Don't have an account?
          <a className="link" href="#signup" onClick={(e) => e.preventDefault()}>
            {" "}Sign Up
          </a>
        </p>
      </div>
    </div>
  );
};

export default Login;