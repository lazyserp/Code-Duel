import { useNavigate } from "react-router-dom";
import "./Landing.css";

function Landing() {
    const navigate = useNavigate();
    const isLoggedIn = !!localStorage.getItem("token");

    return (
        <div className="landing-container">
            {/* Header / Navigation Bar */}
            <header className="landing-header">
                <div className="brand-container" onClick={() => navigate("/")}>
                    <span className="brand-logo-icon"></span>
                    <span className="brand-name">ByteClash</span>
                </div>

                <div className="header-actions">
                    {isLoggedIn ? (
                        <button className="btn-login" onClick={() => navigate("/lobby")}>Dashboard</button>
                    ) : (
                        <button className="btn-login" onClick={() => navigate("/login")}>Log In</button>
                    )}
                </div>
            </header>

            {/* Hero Section */}
            <section className="hero-section">
                <div className="hero-badge">
                    <span>👾</span> Next-Gen Code Battles Powered by AI
                </div>
                <h1 className="hero-title">Next-Gen Coding Battles </h1>
                <p className="hero-subtitle">
                    Experience unparalleled efficiency and precision with Socratic coding tools tailored to streamline 
                    problem-solving, empower competition, and redefine real-time skill tracking.
                </p>
                <button 
                    className="btn-purple-cta" 
                    onClick={() => navigate(isLoggedIn ? "/lobby" : "/register")}
                >
                    {isLoggedIn ? "Go to Lobby" : "Get Started Now"}
                </button>
            </section>

            {/* Premium App/Editor Mockup */}
            <div className="editor-mockup">
                <div className="mockup-header">
                    <div className="window-controls">
                        <span className="dot red"></span>
                        <span className="dot yellow"></span>
                        <span className="dot green"></span>
                    </div>
                    <div className="mockup-address-bar">
                        <span className="lock-icon">🔒</span>
                        <span>https://www.byteclash.ai/arena</span>
                    </div>
                </div>

                <div className="mockup-workspace">
                    {/* Left Sidebar */}
                    <aside className="mockup-sidebar">
                        <div className="sidebar-title">ByteClash V1</div>
                        <div className="sidebar-menu">
                            <div className="sidebar-item active">
                                <span>⚔️</span> Arena mode
                            </div>
                            <div className="sidebar-item">
                                <span>🏆</span> Leaderboard
                            </div>
                            <div className="sidebar-item">
                                <span>🤖</span> Socratic AI
                            </div>
                            <div className="sidebar-item">
                                <span>⚙️</span> Settings
                            </div>
                        </div>
                    </aside>

                    {/* Middle Code Editor */}
                    <main className="mockup-editor">
                        <div className="editor-tabs">
                            <div className="editor-tab active">
                                <span>📝</span> solution.js
                            </div>
                            <div className="editor-tab">
                                <span>🔒</span> test_cases.json
                            </div>
                        </div>
                        <pre className="editor-code">
                            <code>
                                <span className="token comment">// Longest Palindromic Substring Solver</span><br />
                                <span className="token keyword">function</span> <span className="token function">longestPalindrome</span>(s) &#123;<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">let</span> start = <span className="token number">0</span>, end = <span className="token number">0</span>;<br /><br />
                                &nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">for</span> (<span className="token keyword">let</span> i = <span className="token number">0</span>; i &lt; s.length; i++) &#123;<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span className="token comment">// Expand outwards from single character and pair centers</span><br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">let</span> len1 = <span className="token function">expandAroundCenter</span>(s, i, i);<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">let</span> len2 = <span className="token function">expandAroundCenter</span>(s, i, i + <span className="token number">1</span>);<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">let</span> len = Math.<span className="token function">max</span>(len1, len2);<br /><br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">if</span> (len &gt; end - start) &#123;<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;start = i - Math.<span className="token function">floor</span>((len - <span className="token number">1</span>) / <span className="token number">2</span>);<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;end = i + Math.<span className="token function">floor</span>(len / <span className="token number">2</span>);<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&#125;<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;&#125;<br />
                                &nbsp;&nbsp;&nbsp;&nbsp;<span className="token keyword">return</span> s.<span className="token function">substring</span>(start, end + <span className="token number">1</span>);<br />
                                &#125;
                            </code>
                        </pre>
                    </main>

                    {/* Right AI Panel */}
                    <section className="mockup-chat">
                        <div className="chat-header">
                            <span>🤖</span> Socratic AI Assistant
                        </div>
                        <div className="chat-messages">
                            <div className="chat-bubble ai">
                                I notice you are testing substrings one by one. Can we expand outward from centers to improve efficiency?
                            </div>
                            <div className="chat-bubble user">
                                Ah! How do we handle even length palindromes?
                            </div>
                            <div className="chat-bubble ai">
                                For even lengths, think of the center as lying between index i and i+1. Pass those as bounds.
                            </div>
                        </div>
                        <div className="chat-input-area">
                            <div className="chat-mock-input">
                                Ask Socratic Coach...
                            </div>
                            <button className="chat-send-btn">➔</button>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    );
}

export default Landing;
