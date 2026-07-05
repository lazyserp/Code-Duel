import { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Editor } from "@monaco-editor/react";

function Arena() {
    const navigate = useNavigate();
    const [problem, setProblem] = useState(null);
    const [code, setCode] = useState("");
    const [matchStatus, setMatchStatus] = useState("ACTIVE");

    useEffect(() => {
        async function loadMatch() {
            const matchId = localStorage.getItem("matchId");
            const token = localStorage.getItem("token");

            try {
                const response = await axios.get(`http://localhost:8080/api/matchmaking/${matchId}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setProblem(response.data.problem);
                setCode(response.data.problem.starterCode.java || "");
                setMatchStatus(response.data.status);
            } catch (error) {
                console.error("Failed to load match details:", error);
            }
        }
        loadMatch();
    }, []);

    function returnToLobby() {
        localStorage.removeItem("matchId");
        navigate("/lobby");
    }

    if (!problem) {
        return <h1>Loading Problem...</h1>;
    }

    if (matchStatus === "FINISHED") {
        return (
            <>
                <h1>Match Finished!</h1>
                <button onClick={returnToLobby}>Return To Lobby</button>
            </>
        );
    }

    return (
        <>
            <h1>{problem.title}</h1>
            <h2>Difficulty: {problem.difficulty}</h2>
            <p>{problem.description}</p>
            <Editor
                height="500px"
                language="java"
                theme="vs-dark"
                value={code}
                onChange={(val) => setCode(val)}
            />
        </>
    );
}

export default Arena;