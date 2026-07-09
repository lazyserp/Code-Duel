import { useState, useEffect, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Editor } from "@monaco-editor/react";
import { Client } from "@stomp/stompjs";
import "./Arena.css";
import { API_BASE_URL, WS_BASE_URL } from "../config";


    


function renderDescription(text) {
    if (!text) return null;
    const lines = text.split("\n");
    const nodes = [];
    let i = 0;
    let keyCounter = 0;
    const key = () => `desc-${keyCounter++}`;
    



    function formatInline(str) {
        const parts = [];
        const segments = str.split(/(`[^`]+`|\*\*[^*]+\*\*)/g);
        segments.forEach((seg, idx) => {
            if (seg.startsWith("`") && seg.endsWith("`")) {
                parts.push(<code key={idx} className="">{seg.slice(3, -1)}</code>);
            } else if (seg.startsWith("**") && seg.endsWith("**")) {
                parts.push(<strong key={idx}>{seg.slice(2, -2)}</strong>);
            } else {
                parts.push(seg);
            }
        });
        return parts;
    }

    while (i < lines.length) {
        const line = lines[i].trim();
        if (line === "") { i++; continue; }

        if (/^(Example \d+:|Constraints:|Note:|Follow[- ]up:)/i.test(line)) {
            nodes.push(<h3 key={key()} className="problem-section-header">{line}</h3>);
            i++; continue;
        }

        if (/^Input:/i.test(line) || /^Output:/i.test(line) || /^Explanation:/i.test(line)) {
            const blockLines = [];
            while (i < lines.length) {
                const bl = lines[i].trim();
                if (/^(Input:|Output:|Explanation:)/i.test(bl) ||
                    (bl === "" && i + 1 < lines.length && /^(Input:|Output:|Explanation:)/i.test(lines[i + 1].trim()))) {
                    if (bl !== "") blockLines.push(bl);
                    i++;
                } else break;
            }
            nodes.push(
                <div key={key()} className="problem-example-block">
                    {blockLines.map((bl, idx) => {
                        const isInput = /^Input:/i.test(bl);
                        const isOutput = /^Output:/i.test(bl);
                        return (
                            <p key={idx} className={`example-io-line ${isInput ? "input-line" : isOutput ? "output-line" : "explain-line"}`}>
                                {formatInline(bl)}
                            </p>
                        );
                    })}
                </div>
            );
            continue;
        }

        if (/^[-•]/.test(line)) {
            const bullets = [];
            while (i < lines.length && /^[-•]/.test(lines[i].trim())) {
                bullets.push(lines[i].trim().replace(/^[-•]\s*/, ""));
                i++;
            }
            nodes.push(
                <ul key={key()} className="problem-constraints-list">
                    {bullets.map((b, idx) => <li key={idx}>{formatInline(b)}</li>)}
                </ul>
            );
            continue;
        }

        if (line.startsWith("[")) {
            nodes.push(<div key={key()} className="problem-array-display">{line}</div>);
            i++; continue;
        }

        nodes.push(<p key={key()} className="problem-desc-para">{formatInline(line)}</p>);
        i++;
    }
    return nodes;
}

function getInitials(name) {
    return name ? name.slice(0, 2).toUpperCase() : "??";
}

function Arena() {
    const navigate = useNavigate();
    const stompClientRef = useRef(null);
    const defaultCodeRef = useRef("");
    const [submissionError, setSubmissionError] = useState("");
    const [hintError, setHintError] = useState("");

    const [timer, setTimer] = useState(600);
    const [opponentTyping, setOpponentTyping] = useState(false);
    const [problem, setProblem] = useState(null);
    const [code, setCode] = useState("");
    const [matchStatus, setMatchStatus] = useState("ACTIVE");
    const [matchResult, setMatchResult] = useState(null); // "WIN" | "LOSS" | "DRAW"
    const lastTypedRef = useRef(0);
    const [submitting, setSubmitting] = useState(false);
    const [hint, setHint] = useState("");
    const [fetchingHint, setFetchingHint] = useState(false);
    const [submissionResult, setSubmissionResult] = useState(null);
    const [eloDetails, setEloDetails] = useState(null);
    const [opponent, setOpponent] = useState(null);
    const [consoleOpen, setConsoleOpen] = useState(false);
    const [consoleHeight, setConsoleHeight] = useState(200);
    const isResizingRef = useRef(false);
    const startYRef = useRef(0);
    const startHeightRef = useRef(200);

    const username = localStorage.getItem("username") || "Player";
    const elo = localStorage.getItem("elo") || "1200";
    const language = localStorage.getItem("language") || "java";

    const opponentName = opponent ? opponent.username : "Opponent";
    const opponentElo = opponent ? opponent.currentElo : "1200";

    const langDisplayNames = {
        java: "Java",
        python: "Python",
        cpp: "C++"
    };
    const langDisplayName = langDisplayNames[language] || language;

    const handleMouseDown = (e) => {
        e.preventDefault();
        isResizingRef.current = true;
        startYRef.current = e.clientY;
        startHeightRef.current = consoleHeight;
        document.addEventListener("mousemove", handleMouseMove);
        document.addEventListener("mouseup", handleMouseUp);
    };

    const handleMouseMove = (e) => {
        if (!isResizingRef.current) return;
        const newHeight = startHeightRef.current + (startYRef.current - e.clientY);
        if (newHeight > 80 && newHeight < window.innerHeight * 0.7) setConsoleHeight(newHeight);
    };

    const handleMouseUp = () => {
        isResizingRef.current = false;
        document.removeEventListener("mousemove", handleMouseMove);
        document.removeEventListener("mouseup", handleMouseUp);
    };

    function handleResetCode() {
        if (window.confirm("Reset your code to the default starter template?")) {
            setCode(defaultCodeRef.current);
        }
    }

    async function handleGetHint() {
        setFetchingHint(true);
        setHintError("");
        setHint("");

        const token = localStorage.getItem("token");
        const matchId = localStorage.getItem("matchId");
        try {
            const response = await axios.post(
                `${API_BASE_URL}/api/matches/${matchId}/hint`,
                { codeText: code },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setHint(response.data.hintText);
        } catch (error) {
            setHintError(error.response?.data?.message || error.message || "Failed to fetch hint.");
        } finally {
            setFetchingHint(false);
        }
    }

    async function handleSubmitCode() {
        setSubmitting(true);
        setConsoleOpen(true);
        setSubmissionResult(null);
        setSubmissionError("");

        const token = localStorage.getItem("token");
        const matchId = localStorage.getItem("matchId");
        try {
            await axios.post(
                `${API_BASE_URL}/api/submissions`,
                { matchId, codeText: code, language: language },
                { headers: { Authorization: `Bearer ${token}` } }
            );
        } catch (error) {
            setSubmissionError(error.response?.data?.message || error.message || "Failed to submit code.");
            setSubmitting(false);
        }
    }

    function handleCodeChange(value) {
        setCode(value);
        const now = Date.now();
        if (now - lastTypedRef.current > 3000) {
            const matchId = localStorage.getItem("matchId");
            if (stompClientRef.current?.connected) {
                stompClientRef.current.publish({
                    destination: "/app/match/typing",
                    body: JSON.stringify({ matchId }),
                });
                lastTypedRef.current = now;
            }
        }
    }

    const formatTime = (secs) => {
        const m = Math.floor(secs / 60).toString().padStart(2, "0");
        const s = (secs % 60).toString().padStart(2, "0");
        return `${m}:${s}`;
    };

    useEffect(() => {
        const matchId = localStorage.getItem("matchId");
        if (!matchId) { navigate("/lobby"); return; }
        async function loadMatch() {
            const token = localStorage.getItem("token");
            try {
                const response = await axios.get(
                    `${API_BASE_URL}/api/matchmaking/${matchId}`,
                    { headers: { Authorization: `Bearer ${token}` } }
                );
                setProblem(response.data.problem);
                
                const currentUser = localStorage.getItem("username");
                const oppUser = response.data.user1.username === currentUser ? response.data.user2 : response.data.user1;
                setOpponent(oppUser);

                const starterCodeObj = JSON.parse(response.data.problem.starterCode);
                const starterForLang = starterCodeObj[language] || "";
                defaultCodeRef.current = starterForLang;
                setCode(starterForLang);
                setMatchStatus(response.data.status);
            } catch {
                navigate("/lobby");
            }
        }
        loadMatch();
    }, []);

    useEffect(() => {
        const matchId = localStorage.getItem("matchId");
        if (!matchId) return;
        const token = localStorage.getItem("token");
        const stompClient = new Client();
        stompClient.brokerURL = `${WS_BASE_URL}?token=${token}`;
        stompClient.reconnectDelay = 5000;

        const callBack = (message) => {
            const data = JSON.parse(message.body);
            if (data.winnerId !== undefined) {
                const myUserId = localStorage.getItem("userId");
                const isWin = data.winnerId == myUserId;
                setMatchResult(isWin ? "WIN" : "LOSS");
                setMatchStatus("FINISHED");

                const change = isWin ? data.winnerEloChange : data.loserEloChange;
                const before = isWin ? data.winnerEloBefore : data.loserEloBefore;
                const after = isWin ? data.winnerEloAfter : data.loserEloAfter;
                setEloDetails({ change, before, after });
                localStorage.setItem("elo", after.toString());
            }
            if (data.status !== undefined && data.status !== "ACTIVE" && data.userId == localStorage.getItem("userId")) {
                setSubmissionResult({ status: data.status, passedCount: data.passedCount, totalCount: data.totalCount, executionTime: data.executionTime });
                setSubmitting(false);
                setConsoleOpen(true);
            }
            if (data.secondsRemaining === 0) { setMatchResult("DRAW"); setMatchStatus("FINISHED"); }
            if (data.secondsRemaining !== undefined) setTimer(parseInt(data.secondsRemaining, 10));
            if (data.status !== undefined) setMatchStatus(data.status);
            if (data.username !== undefined && data.username !== localStorage.getItem("username")) {
                setOpponentTyping(true);
                setTimeout(() => setOpponentTyping(false), 2000);
            }
        };

        stompClient.onConnect = () => {
            stompClient.subscribe(`/topic/match/${matchId}`, callBack);
            stompClient.publish({ destination: "/app/match/ready", body: JSON.stringify({ matchId }) });
        };
        stompClient.activate();
        stompClientRef.current = stompClient;
        return () => {
            stompClient.deactivate();
        };
    }, []);

    function returnToLobby() {
        localStorage.removeItem("matchId");
        navigate("/lobby");
    }

    function handleForfeit() {
        if (window.confirm("Are you sure you want to Give up? You will lose ELO.")) {
            const matchId = localStorage.getItem("matchId");
            if (stompClientRef.current?.connected) {
                stompClientRef.current.publish({ destination: "/app/match/forfeit", body: JSON.stringify({ matchId }) });
            } else returnToLobby();
        }
    }

    if (!problem) {
        return (
            <div className="arena-loading">
                <div className="spinner"></div>
                <p>Loading Arena...</p>
            </div>
        );
    }

    if (matchStatus === "FINISHED") {
        const results = {
            WIN:  { text: "You Won! 🎉",      sub: "Excellent work!",              color: "#10b981" },
            LOSS: { text: "You Lost 😔",       sub: "Better luck next time.",       color: "#ef4444" },
            DRAW: { text: "Match Drawn ⏱",    sub: "Time ran out  no one solved it.", color: "#e2b13c" },
        };
        const r = results[matchResult] || results.DRAW;
        return (
            <div className="finished-container">
                <h1 className="finished-title" style={{ color: r.color, WebkitTextFillColor: r.color }}>{r.text}</h1>
                <p style={{ color: "#888", fontSize: "0.9rem", margin: 0, marginBottom: "15px" }}>{r.sub}</p>
                {eloDetails && (
                    <div className="elo-change-card" style={{ background: "#252526", border: "1px solid #3c3c3c", borderRadius: "8px", padding: "16px", marginBottom: "20px", display: "flex", flexDirection: "column", gap: "8px", alignItems: "center" }}>
                        <span style={{ fontSize: "0.85rem", color: "#888" }}>Rating Change</span>
                        <div style={{ display: "flex", alignItems: "center", gap: "12px", fontSize: "1.1rem" }}>
                            <span>{eloDetails.before}</span>
                            <span style={{ color: "#666" }}>→</span>
                            <strong style={{ color: r.color }}>{eloDetails.after}</strong>
                        </div>
                        <span style={{ fontSize: "1.2rem", fontWeight: "700", color: eloDetails.change >= 0 ? "#10b981" : "#ef4444" }}>
                            {eloDetails.change >= 0 ? `+${eloDetails.change}` : eloDetails.change} ELO
                        </span>
                    </div>
                )}
                <button className="btn-secondary" onClick={returnToLobby}>Return To Lobby</button>
            </div>
        );
    }

    const timerWarning = timer <= 60;

    return (
        <div className="arena-container">
            {/* ── Top Navbar ── */}
            <div className="arena-navbar">
                {/* Left: current user */}
                <div className="nav-player-section">
                    <div className="player-avatar you">{getInitials(username)}</div>
                    <div className="player-info">
                        <span className="player-name">{username}</span>
                        <div className="player-meta">
                            <span className="player-rank-badge">⚔ Duelist</span>
                            <span className="player-elo-badge">⭐ {elo}</span>
                        </div>
                    </div>
                    <div className="player-progress">
                        <span className="progress-label">0 / {problem ? "?" : "?"} tests</span>
                        <div className="progress-bar"><div className="progress-fill" style={{ width: "0%" }}></div></div>
                    </div>
                </div>

                {/* Center: timer */}
                <div className="nav-center">
                    <div className={`timer-display ${timerWarning ? "timer-warning" : ""}`}>
                        {formatTime(timer)}
                    </div>
                </div>

                {/* Right: opponent */}
                <div className="nav-player-section right">
                    <div className="player-progress right-progress">
                        <span className="progress-label">0 / {problem ? "?" : "?"} tests</span>
                        <div className="progress-bar"><div className="progress-fill" style={{ width: "0%" }}></div></div>
                    </div>
                    <div className="player-info right-info">
                        <span className="player-name">{opponentName}</span>
                        <div className="player-meta">
                            <span className="player-elo-badge">⭐ {opponentElo}</span>
                            <span className={`idle-badge ${opponentTyping ? "typing" : ""}`}>
                                {opponentTyping ? "● TYPING" : "● IDLE"}
                            </span>
                        </div>
                    </div>
                    <div className="player-avatar opponent">{getInitials(opponentName)}</div>
                </div>
            </div>

            {/* ── Sub-header: actions + hint nudge ── */}
            <div className="arena-subheader">
                <div className="subheader-left">
                    <button className="action-btn" onClick={() => window.location.href = "mailto:amanjha434@gmail.com?subject=CodeDuel%20Bug%20Report"}>
                        ⚠ Report bug
                    </button>
                    <button className="action-btn forfeit-btn" onClick={handleForfeit}>
                        ⚑ Give Up
                    </button>
                </div>
                <div className="subheader-center">
                    <div className="hint-nudge-bar">
                        <span className="hint-nudge-icon"></span>
                        <span className="hint-nudge-label">⚡ Hint</span>
                        
                        <button
                            className="btn-get-hint"
                            onClick={handleGetHint}
                            disabled={fetchingHint || matchStatus === "FINISHED"}
                        >
                            {fetchingHint ? <><span className="hint-spinner"></span> Thinking...</> : "Get a hint"}
                        </button>
                    </div>
                </div>
                <div className="subheader-right"></div>
            </div>

            {/* ── Main workspace ── */}
            <div className="arena-workspace">
                {/* Left: Problem Panel */}
                <div className="problem-panel">
                    <div className="problem-panel-body">
                        <h1 className="problem-title">{problem.title}</h1>
                        <div className="problem-tags">
                            <span className={`tag-difficulty ${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span>
                            {hint && <span className="tag hint-tag">💡 Hints</span>}
                        </div>
                        {hintError && (
                        <div className="ai-hint-box error">
                            <div className="ai-hint-title">💡 AI Coach Error</div>
                            <div className="ai-hint-body">{hintError}</div>
                        </div>
                    )}


                        <div className="problem-description">
                            {renderDescription(problem.description)}
                        </div>

                        {hint && (
                            <div className="ai-hint-box">
                                <div className="ai-hint-title">💡 AI Coach Hint</div>
                                <div className="ai-hint-body">{hint}</div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Divider */}
                <div className="panel-divider"></div>

                {/* Right: Editor Panel */}
                <div className="editor-panel">
                    {/* Editor toolbar */}
                    <div className="editor-toolbar">
                        <div className="toolbar-left">
                            <span className="lang-badge">{langDisplayName}</span>
                            <span className="toolbar-dot">●</span>
                            <span className="toolbar-auto">Auto</span>
                        </div>
                        <div className="toolbar-right">
                            <button className="toolbar-icon-btn" onClick={handleResetCode} title="Reset code" disabled={matchStatus === "FINISHED"}>↺</button>
                        </div>
                    </div>

                    {/* Monaco Editor */}
                    <div className="editor-wrapper">
                        <Editor
                            height="100%"
                            language={language}
                            theme="vs-dark"
                            value={code}
                            onChange={handleCodeChange}
                            options={{
                                fontSize: 14,
                                minimap: { enabled: false },
                                automaticLayout: true,
                                fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                                fontLigatures: true,
                                scrollBeyondLastLine: false,
                                tabSize: 4,
                                lineNumbers: "on",
                            }}
                            {submissionError && <div className="console-error-message">{submissionError}</div>}

                        />
                    </div>

                    {/* Console Drawer */}
                    {consoleOpen && (
                        <div className="console-drawer" style={{ height: `${consoleHeight}px` }}>
                            <div className="console-resize-handle" onMouseDown={handleMouseDown} />
                            <div className="console-header">
                                <span>Console</span>
                                <button className="btn-close-console" onClick={() => setConsoleOpen(false)}>×</button>
                            </div>
                            <div className="console-body">
                                {submitting ? (
                                    <p className="status-running">Running test cases...</p>
                                ) : submissionResult ? (
                                    <div className="console-results">
                                        <span className={`status-badge ${submissionResult.status.toLowerCase()}`}>
                                            {submissionResult.status.replace(/_/g, " ")}
                                        </span>
                                        <div className="console-stats">
                                            <span>Tests: {submissionResult.passedCount} / {submissionResult.totalCount}</span>
                                            <span>Time: {submissionResult.executionTime} ms</span>
                                        </div>
                                    </div>
                                ) : (
                                    <p className="console-placeholder">No output yet.</p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Footer */}
                    <div className="editor-footer">
                        <button className="btn-console-toggle" onClick={() => setConsoleOpen(!consoleOpen)}>
                            Console {consoleOpen ? "∧" : "∨"}
                        </button>
                        <div className="footer-actions">
                            <button className="btn-run" onClick={handleSubmitCode} disabled={submitting}>Run</button>
                            <button className="btn-submit" onClick={handleSubmitCode} disabled={submitting}>
                                {submitting ? "Evaluating..." : "Submit"}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Arena;
