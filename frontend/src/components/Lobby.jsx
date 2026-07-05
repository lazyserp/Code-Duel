import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

function Lobby() {
    const navigate = useNavigate();
    const [difficulty, setDifficulty] = useState("EASY");
    const [status, setStatus] = useState("");
    const username = localStorage.getItem("username");

    function startCountdown() {
        let count = 5;
        setStatus("Match found! Entering Arena in 5...");
        const timerId = setInterval(() => {
            count--;
            if (count > 0) {
                setStatus("Match found! Entering Arena in " + count + "...");
            } else {
                clearInterval(timerId);
                navigate("/arena");
            }
        }, 1000);
    }

    async function handleJoin() {
        try {
            const token = localStorage.getItem("token");
            const userId = localStorage.getItem("userId");

            const response = await axios.post(
                "http://localhost:8080/api/matchmaking/join",
                { userId, difficulty },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            if (response.data.status === "QUEUED") {
                setStatus("Queued! Waiting for opponent...");

                const intervalId = setInterval(async () => {
                    const resp = await axios.get(
                        `http://localhost:8080/api/matchmaking/active?userId=${userId}`,
                        { headers: { Authorization: `Bearer ${token}` } }
                    );
                    if (resp.data) {
                        localStorage.setItem("matchId", resp.data.id);
                        clearInterval(intervalId);
                        startCountdown();
                    }
                }, 2000);
            } else {
                localStorage.setItem("matchId", response.data.id);
                setStatus("Match found! Redirecting...");
                startCountdown();
            }
        } catch (error) {
            alert("Error occurred: " + error.message);
        }
    }

    return (
        <>
            <h2>Welcome, {username}</h2>
            <select value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
                <option value="EASY">EASY</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HARD">HARD</option>
            </select>
            <p>{status}</p>
            <button onClick={handleJoin}>Join Match</button>
        </>
    );
}

export default Lobby;