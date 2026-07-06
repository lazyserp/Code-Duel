import { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Lobby.css"

function Lobby() {
    const navigate = useNavigate();
    const [difficulty, setDifficulty] = useState("EASY");
    const [status, setStatus] = useState("");
    const username = localStorage.getItem("username");
    const [elo,setElo] = useState(null)

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

    useEffect( () => 
    {
        async function fetchData()
        {

        const token = localStorage.getItem("token")
        try
        {
            const profileResponse = await axios.get("http://localhost:8080/api/users/me", {headers : {Authorization: `Bearer ${token}`}});
            setElo(profileResponse.data.currentElo)
        }
        catch (error)
        {
            alert("not found" + error.message)
        }

        const userId = localStorage.getItem("userId");

        async function checkForExistingMatch()
        {
            try{
                const MatchResponse = await axios.get("http://localhost:8080/api/matchmaking/active?userId="+userId , 
                                                    {headers : { Authorization : `Bearer ${token}`}});
            if (MatchResponse.data){
                localStorage.setItem("matchId",MatchResponse.data.id);
                navigate("/arena");
            }
            }
            finally
            {
                console.log("Sending to Arena !")
            }
        }

        checkForExistingMatch()

        }
        fetchData();
    },[])
    

    return (
        <div className="lobby-container">
            <h1 className="lobby-title">Lobby</h1>
            <p className="lobby-subtitle">Enter matchmaking for a 1v1 with a developer</p>

            <div className="lobby-profile">
                <div className="profile-item">
                    <span className="profile-label">Competitor</span>
                    <span className="profile-value">{username}</span>
                </div>
                <div className="profile-item">
                    <span className="profile-label">Rating</span>
                    <span className="profile-value gold">{elo !== null ? elo : "Loading..."}</span>
                </div>
            </div>

            <div className="lobby-form">
                <div className="form-group">
                    <label className="form-label" htmlFor="difficulty-select">Select Difficulty</label>
                    <select 
                        id="difficulty-select"
                        className="lobby-select" 
                        value={difficulty} 
                        onChange={(e) => setDifficulty(e.target.value)}
                    >
                        <option value="EASY">EASY</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HARD">HARD</option>
                    </select>
                </div>

                <p className="lobby-status">{status || "Ready for battle"}</p>

                <div className="lobby-actions">
                    <button className="btn-primary" onClick={handleJoin}>Join Match</button>
                    <button className="btn-secondary" onClick={() => navigate("/leaderboard")}>View Leaderboard</button>
                </div>
            </div>
        </div>
        
    );
}

export default Lobby;