import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Auth.css";


import { API_BASE_URL } from "../config";

function Register() {
    const navigate = useNavigate();
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error,setError] = useState("")

    async function handleSubmit(e) {
        e.preventDefault();
        setError("")
        try {
            await axios.post(`${API_BASE_URL}/api/auth/register`, {
                username,
                email,
                password
            });
            navigate("/login");
        } catch (err) {
            // Safely query response fields
            setError(err.response?.data?.message || err.message || "Something went wrong");
        }
    }

    return (
        <div className="auth-container">
            <h1 className="auth-title">Create Account</h1>
            <p className="auth-subtitle">Join the competitive coding arena</p>
            {error && <div className="auth-error-box"> {error} </div>}
            <form onSubmit={handleSubmit} className="auth-form">
                <div className="form-group">
                    <label className="form-label" htmlFor="username-input">Username</label>
                    <input
                        id="username-input"
                        className="auth-input"
                        type="text"
                        placeholder="Choose a username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label className="form-label" htmlFor="email-input">Email</label>
                    <input
                        id="email-input"
                        className="auth-input"
                        type="email"
                        placeholder="Enter your email address"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label className="form-label" htmlFor="password-input">Password</label>
                    <input
                        id="password-input"
                        className="auth-input"
                        type="password"
                        placeholder="Create a secure password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <button type="submit" className="btn-primary">Register</button>
            </form>
            <div className="auth-link-container">
                Already have an account? 
                <span className="auth-link" onClick={() => navigate("/login")}>
                    Log in here
                </span>
            </div>
        </div>
    );
}

export default Register;