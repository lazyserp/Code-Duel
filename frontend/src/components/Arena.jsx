import { useState, useEffect, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Editor } from "@monaco-editor/react";
import { Client } from "@stomp/stompjs";
import "./Arena.css";

function Arena() {
    const navigate = useNavigate();
    const stompClientRef = useRef(null);
    const [timer ,setTimer] = useState(600);
    const [opponentTyping, setOpponentTyping] = useState(false);
    const [problem, setProblem] = useState(null);
    const [code, setCode] = useState("");
    const [matchStatus, setMatchStatus] = useState("ACTIVE");
    const lastTypedRef = useRef(0);
    const [submitting, setSubmitting] = useState(false);
    const [hint, setHint] = useState("");
    const [fetchingHint, setFetchingHint] = useState(false);
    const [submissionResult, setSubmissionResult] = useState(null);
    const [consoleOpen, setConsoleOpen] = useState(false);
    const [consoleHeight, setConsoleHeight] = useState(200);
    const isResizingRef = useRef(false);

    const handleMouseDown = (e) => {
        e.preventDefault();
        isResizingRef.current = true;
        document.addEventListener("mousemove", handleMouseMove);
        document.addEventListener("mouseup", handleMouseUp);
    };

    const handleMouseMove = (e) => {
        if (!isResizingRef.current) return;
        const newHeight = window.innerHeight - e.clientY;
        if (newHeight > 100 && newHeight < window.innerHeight * 0.8) {
            setConsoleHeight(newHeight);
        }
    };

    const handleMouseUp = () => {
        isResizingRef.current = false;
        document.removeEventListener("mousemove", handleMouseMove);
        document.removeEventListener("mouseup", handleMouseUp);
    };

    async function handleGetHint()
    {
        setFetchingHint(true);
        const token = localStorage.getItem("token");
        const matchId = localStorage.getItem("matchId");

        try{

        const response = await axios.post(`http://localhost:8080/api/matches/${matchId}/hint`,
                                        {codeText:code},
                                        {headers : {Authorization: `Bearer ${token}`}});
        setHint(response.data.hintText);
        
        }
        catch (error)
        {
            alert("Error :" + error.message);
        }
        finally{
            setFetchingHint(false);


        }

        
    }

    async function handleSubmitCode()
    {
        setSubmitting(true);
        setConsoleOpen(true);
        setSubmissionResult(null);
        const token = localStorage.getItem("token");
        const matchId = localStorage.getItem("matchId");

        try{
            const response = await axios.post("http://localhost:8080/api/submissions",
                                            {matchId,codeText:code,language:"java"},
                                            {headers : {Authorization: `Bearer ${token}`} });
        }
        catch (error)
        {
            alert(error.message);
            setSubmitting(false);

        }


    }
    function handleCodeChange(value)
    {
        setCode(value);
        const now = Date.now();
        if ( now - lastTypedRef.current > 3000)
        {
            const matchId = localStorage.getItem("matchId");
            if ( stompClientRef.current &&  stompClientRef.current.connected )
            {
                stompClientRef.current.publish({destination: "/app/match/typing",body: JSON.stringify({matchId})})
                lastTypedRef.current = now;
            }

        }
    }

    const formatTime = (secs) => {
        const mins = Math.floor(secs / 60);
        const remainingSecs = secs % 60;
        return `${mins.toString().padStart(2,'0')} : ${remainingSecs.toString().padStart(2,'0')}`;
    }

    // this hook is for setting the problem UI
    useEffect(() => {
        const matchId = localStorage.getItem("matchId");
        if (!matchId) {
            navigate("/lobby");
            return;
        }
        async function loadMatch() {
            const token = localStorage.getItem("token");

            try {
                const response = await axios.get(`http://localhost:8080/api/matchmaking/${matchId}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setProblem(response.data.problem);

                const starterCodeObj = JSON.parse(response.data.problem.starterCode);
                setCode(starterCodeObj.java || "");

                setMatchStatus(response.data.status);
            } catch (error) {
                console.error("Failed to load match details:", error);
                navigate("/lobby")
            }
        }
        loadMatch();
    }, []);

    // this hook is for our Websocket connection , msg sending and receiving
    useEffect(() => {
        const matchId = localStorage.getItem("matchId");
        if (!matchId) return;
        const token = localStorage.getItem("token");

        const stompClient = new Client();
        stompClient.brokerURL = `ws://localhost:8080/ws?token=${token}`;
        stompClient.reconnectDelay = 5000;


        //This runs whenver our WebSocket receives a message on the connected URL
        const callBack = (message) => {
            const data = JSON.parse(message.body);
            if ( data.winnerId !== undefined)
            {
                setMatchStatus("FINISHED");
                if ( data.winnerId == localStorage.getItem("userId")){
                    alert("You Won !")
                }
                else alert("You Lost")
            }

            if ( data.status !== undefined && data.status != "ACTIVE" && data.userId == localStorage.getItem("userId"))
            {
                setSubmissionResult({
                    status: data.status,
                    passedCount: data.passedCount,
                    totalCount: data.totalCount,
                    executionTime: data.executionTime
                });
                setSubmitting(false);
                setConsoleOpen(true);
            }
            if (data.secondsRemaining == 0)
            {
                setMatchStatus("FINISHED");
            }
            if ( data.secondsRemaining  !== undefined)
            {
               setTimer(parseInt(data.secondsRemaining, 10));
            }

            if ( data.status != undefined)
            {
                setMatchStatus(data.status);
            }

            if ( data.username !== undefined && data.username != localStorage.getItem("username"))
            {
                setOpponentTyping(true);
                setTimeout( () => setOpponentTyping(false),2000);
            }
        }

        stompClient.onConnect = () => {
            stompClient.subscribe(`/topic/match/${matchId}`, callBack)
            stompClient.publish({destination: "/app/match/ready",body: JSON.stringify({ matchId }) });
        } 

        stompClient.activate();
        stompClientRef.current = stompClient;
        return () => { stompClient.deactivate(); };
    },[]);



    function returnToLobby() {
        localStorage.removeItem("matchId");
        navigate("/lobby");
    }

    function handleForfeit() {
        if (window.confirm("Are you sure you want to forfeit this match? You will lose ELO.")) {
            const matchId = localStorage.getItem("matchId");
            if (stompClientRef.current && stompClientRef.current.connected) {
                stompClientRef.current.publish({
                    destination: "/app/match/forfeit",
                    body: JSON.stringify({ matchId })
                });
            } else {
                returnToLobby();
            }
        }
    }

    if (!problem) {
        return (
            <div className="leaderboard-loading">
                <div className="spinner"></div>
                <p>Loading Arena Workspace...</p>
                
            </div>
        );
    }

    if (matchStatus === "FINISHED") {
        return (
            <div className="finished-container">
                <h1 className="finished-title">Match Finished!</h1>
                <button className="btn-secondary" onClick={returnToLobby}>Return To Lobby</button>
            </div>
        );
    }

    return (
        <div className="arena-container">
            {/* 1. Header Navbar */}
            <div className="arena-navbar">
                <div className="nav-section">
                    <span className="logo-text">CodeDuel</span>
                    <div className="nav-user-card">
                        <span className="user-dot"></span>
                        <span className="nav-username">{localStorage.getItem("username")}</span>
                        <span className="nav-elo">{localStorage.getItem("elo") || "1200"} ELO</span>
                    </div>
                </div>

                <div className="timer-container">
                    <div className="timer-box">{formatTime(timer)}</div>
                </div>

                <div className="nav-section">
                    {opponentTyping && <span style={{ color: "#38bdf8", fontSize: "0.8rem", marginRight: "10px" }}>Opponent typing...</span>}
                    <div className="nav-user-card">
                        <span className="user-dot opponent"></span>
                        <span className="nav-username">Opponent</span>
                    </div>
                </div>
            </div>

            {/* 2. Actions Sub-header Row */}
            <div className="arena-actions-row">
                <button className="action-link" onClick={handleForfeit}>Forfeit Match</button>
                <span style={{ color: "#2d2d2d" }}>|</span>
                <button className="action-link">Report Bug</button>
            </div>

            {/* 3. Main Workspace Split-Screen */}
            <div className="arena-workspace">
                {/* Left Panel: Problem description */}
                <div className="workspace-panel">
                    <div className="panel-header">
                         <span className="panel-title">Problem Description</span>
                    </div>
                    <div className="panel-body">
                         <div className="problem-title-row">
                             <h1>{problem.title}</h1>
                             <span className={`difficulty-badge ${problem.difficulty.toLowerCase()}`}>
                                 {problem.difficulty}
                             </span>
                         </div>
                         <p className="problem-desc">{problem.description}</p>

                         {/* AI Hint Section */}
                         <div style={{ marginTop: "30px" }}>
                             <button 
                                 className="btn-run" 
                                 onClick={handleGetHint} 
                                 disabled={fetchingHint || matchStatus === "FINISHED"}
                             >
                                 {fetchingHint ? "AI is thinking..." : "Request Socratic Hint"}
                             </button>
                             
                             {hint && (
                                 <div className="ai-hint-box">
                                     <div className="ai-hint-title">
                                         💡 AI Coach Hint
                                     </div>
                                     <div>{hint}</div>
                                 </div>
                             )}
                         </div>
                    </div>
                </div>

                {/* Right Panel: Code Editor */}
                <div className="workspace-panel">
                    <div className="panel-header">
                        <span className="panel-title">Code Editor</span>
                        <select className="editor-select" value="java" disabled>
                             <option value="java">Java 21</option>
                        </select>
                    </div>
                    <div className="editor-wrapper">
                         <Editor
                             height="100%"
                             language="java"
                             theme="vs-dark"
                             value={code}
                             onChange={handleCodeChange}
                             options={{
                                 fontSize: 14,
                                 minimap: { enabled: false },
                                 automaticLayout: true
                             }}
                         />
                    </div>

                    {/* Console Drawer */}
                    {consoleOpen && (
                        <div className="console-drawer" style={{ height: `${consoleHeight}px` }}>
                            <div className="console-resize-handle" onMouseDown={handleMouseDown} />
                            <div className="console-drawer-header">
                                <span>Console Output</span>
                                <button className="btn-close-console" onClick={() => setConsoleOpen(false)}>×</button>
                            </div>
                            <div className="console-drawer-body">
                                {submitting ? (
                                    <p className="status-running">Running code and evaluating test cases...</p>
                                ) : submissionResult ? (
                                    <div className="console-results">
                                        <span className={`status-badge ${submissionResult.status.toLowerCase()}`}>
                                            {submissionResult.status.replace("_", " ")}
                                        </span>
                                        <div className="console-stats">
                                            <span>Tests Passed: {submissionResult.passedCount} / {submissionResult.totalCount}</span>
                                            <span>Execution Time: {submissionResult.executionTime} ms</span>
                                        </div>
                                    </div>
                                ) : (
                                    <p className="console-placeholder" style={{ color: "#64748b", margin: 0 }}>
                                        No output yet. Run or Submit your code to see results.
                                    </p>
                                )}
                            </div>
                        </div>
                    )}

                    <div className="editor-footer">
                         <div className="footer-left">
                             <button className="btn-console" onClick={() => setConsoleOpen(!consoleOpen)}>Console</button>
                         </div>
                         <div className="footer-right">
                             <button className="btn-run" onClick={handleSubmitCode} disabled={submitting}>Run</button>
                             <button 
                                 className="btn-submit" 
                                 onClick={handleSubmitCode} 
                                 disabled={submitting}
                             >
                                 {submitting ? "Evaluating..." : "Submit Code"}
                             </button>
                         </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Arena;