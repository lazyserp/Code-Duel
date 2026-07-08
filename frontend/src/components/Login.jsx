import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Auth.css";

function Login() {
    const navigate = useNavigate();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    async function handleSubmit(e) {
        e.preventDefault();
        try {
            const response = await axios.post("http://localhost:8080/api/auth/login", {
                username,
                password
            });
            localStorage.setItem("token", response.data.token);
            localStorage.setItem("username", response.data.username);
            localStorage.setItem("userId", response.data.userId);
            navigate("/lobby");
        } catch (error) {
            alert("Error: Login failed!");
        }
    }

    return (
        <div className="auth-container">
            <h1 className="auth-title">Welcome Back</h1>
            <p className="auth-subtitle">Sign in to your CodeDuel account</p>
            <form onSubmit={handleSubmit} className="auth-form">
                <div className="form-group">
                    <label className="form-label" htmlFor="username-input">Username</label>
                    <input
                        id="username-input"
                        className="auth-input"
                        type="text"
                        placeholder="Enter your username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label className="form-label" htmlFor="password-input">Password</label>
                    <input
                        id="password-input"
                        className="auth-input"
                        type="password"
                        placeholder="Enter your password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <button type="submit" className="btn-primary">Log In</button>
            </form>
            <div className="auth-link-container">
                Don't have an account? 
                <span className="auth-link" onClick={() => navigate("/register")}>
                    Register here
                </span>
            </div>
        </div>
    );
}

export default Login;