import { useState, useEffect } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Leaderboard.css";

function Leaderboard() {
    const navigate = useNavigate();
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        async function fetchLeaderboard() {
            try {
                const token = localStorage.getItem("token");
                const response = await axios.get("http://localhost:8080/api/leaderboard", {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setData(response.data);
            } catch (err) {
                console.error("Error fetching leaderboard:", err);
                setError(err.message || "Failed to load leaderboard");
            } finally {
                setLoading(false);
            }
        }
        fetchLeaderboard();
    }, []);

    const formatTrend = (trend) => {
        if (trend === "UP") {
            return <span className="trend-arrow trend-up">▲</span>;
        } else if (trend === "DOWN") {
            return <span className="trend-arrow trend-down">▼</span>;
        }
        return <span className="trend-arrow trend-stagnant">—</span>;
    };


    const getStreakStyle = (streak) => {
        if (streak && streak.startsWith("W")) return "streak-badge streak-win";
        if (streak && streak.startsWith("L")) return "streak-badge streak-loss";
        return "streak-badge";
    };

    if (loading) {
        return (
            <div className="leaderboard-loading">
                <div className="spinner"></div>
                <p>Loading Arena Rankings...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="leaderboard-container">
                <h2>Leaderboard Error</h2>
                <p style={{ color: "#ef4444" }}>{error}</p>
                
            </div>
        );
    }

    const topPlayers = data?.topPlayers || [];
    const currentUserRanking = data?.currentUserRanking;

    // Derived Stats
    // const totalPlayersCount = topPlayers.length;
    const highestEloValue = topPlayers[0]?.elo || 1200;

    return (
        <div className="leaderboard-container">
            {/* Header */}
            <div className="leaderboard-header">
                <div className="title-section" style={{ textAlign: "center" }}>
                    <h1>Leaderboard</h1>
                    <p>Top duelists ranked by rating</p>
                </div>
            </div>

            {/* Leaderboard Table */}
            <div className="table-wrapper">
                <table className="leaderboard-table">
                    <thead>
                        <tr>
                            <th>Rank</th>
                            <th>Player</th>
                            <th>Rating</th>
                            <th>W - L</th>
                            <th>Win %</th>
                            <th>Streak</th>
                            <th>Trend</th>
                        </tr>
                    </thead>
                    <tbody>
                        {topPlayers.map((player) => (
                            <tr key={player.username}>
                                <td>
                                    <span className={player.rank}> {player.rank} </span>
                                </td>
                                <td>
                                    <div className="player-info">
                                        <span className="player-name">{player.username}</span>
                                    </div>
                                </td>
                                <td>
                                    <span className="elo-value">{player.elo}</span>
                                </td>
                                <td>{player.wins} - {player.losses}</td>
                                <td>{player.winRate}%</td>
                                <td>
                                    <span className={getStreakStyle(player.streak)}>
                                        {player.streak}
                                    </span>
                                </td>
                                <td>{formatTrend(player.trend)}</td>
                            </tr>
                        ))}
                        {topPlayers.length === 0 && (
                            <tr>
                                <td colSpan={7} style={{ textAlign: "center", color: "#64748b", padding: "30px" }}>
                                    No players found. Join the queue to make history!
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
            

            {/* Sticky Current User Ranking Card */}
            {currentUserRanking && (
                <div className="current-user-card">
                    <div className="user-card-header">
                        <span className="user-card-title">Your Global Position</span>
                        
                        <span className="user-card-motto">Keep pushing, Legend! </span>
                    </div>
                
                    <div className="user-card-row">
                        <div className="user-card-left">
                            <span className="user-rank">#{currentUserRanking.rank}</span>
                            <div className="player-info">
                                <span className="player-name">{currentUserRanking.username} (You)</span>
                            </div>
                        </div>
                        <div className="user-card-right">
                            <div className="user-stat-item">
                                <span className="user-stat-val elo-value">{currentUserRanking.elo}</span>
                                <span className="user-stat-lbl">Rating</span>
                            </div>
                            <div className="user-stat-item">
                                <span className="user-stat-val">{currentUserRanking.wins} - {currentUserRanking.losses}</span>
                                <span className="user-stat-lbl">Record (W-L)</span>
                            </div>
                            <div className="user-stat-item">
                                <span className="user-stat-val">{currentUserRanking.winRate}%</span>
                                <span className="user-stat-lbl">Win Rate</span>
                            </div>
                            <div className="user-stat-item">
                                <span className={getStreakStyle(currentUserRanking.streak)}>
                                    {currentUserRanking.streak}
                                </span>
                                <span className="user-stat-lbl">Streak</span>
                            </div>
                            <div className="user-stat-item" style={{ alignItems: "center" }}>
                                {formatTrend(currentUserRanking.trend)}
                                <span className="user-stat-lbl">Trend</span>
                            </div>
                        </div>
                    </div>
                    
                </div>
                
                
                
                
            )}
            <button className="back-btn" onClick={() => navigate("/lobby")}>
                    ← Back to Lobby
                </button>
        </div>
        
    );
}

export default Leaderboard;
