import { useState, useEffect, useRef } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Editor } from "@monaco-editor/react";
import { Client } from "@stomp/stompjs";

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

    async function handleSubmitCode()
    {
        setSubmitting(true);
        const token = localStorage.getItem("token")
        const matchId = localStorage.getItem("matchId");

        try{
            const response = await axios.post("http://localhost:8080/api/submissions",
                                            {matchId,codeText:code,language:"java"},
                                            {headers : {Authorization: `Bearer ${token}`} });
        }
        catch (error)
        {
            alert(error.message)
        }
        setSubmitting(false);


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
        async function loadMatch() {
            const matchId = localStorage.getItem("matchId");
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
            }
        }
        loadMatch();
    }, []);

    // this hook is for our Websocket connection , msg sending and receiving
    useEffect(() => {
        const matchId = localStorage.getItem("matchId");
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
                localStorage.removeItem("matchId");
                if ( data.winnerId == localStorage.getItem("userId")){
                    alert("You Won !")
                }
                else alert("You Lost")
            }

            if ( data.status !== undefined && data.status != "ACTIVE" && data.userId == localStorage.getItem("userId"))
            {
                alert("Code run Status: " + data.status)

            }
            if (data.secondsRemaining == 0)
            {
                setMatchStatus("FINISHED");
                localStorage.removeItem("matchId");       
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

            <h3> Time Remaining : {formatTime(timer)} </h3>
            {opponentTyping && <p> Opponent is Typing...</p>}

            <Editor
                height="500px"
                language="java"
                theme="vs-dark"
                value={code}
                onChange={handleCodeChange}
            />

            <button onClick={handleSubmitCode} disabled={submitting}> {submitting ? "Running Tests..." : "Submit Code"}</button>
        </>
    );
}

export default Arena;