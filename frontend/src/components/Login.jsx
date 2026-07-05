import axios from "axios";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login()
{
    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")

    const navigate = useNavigate()
    async function handleSubmit(e)
    {
        e.preventDefault()
        try
        {
            const response = await axios.post("http://localhost:8080/api/auth/login",{username , password})
            localStorage.setItem("token",response.data.token)
            localStorage.setItem("username",response.data.username)
            localStorage.setItem('userId', response.data.userId)

            navigate("/lobby")
        }
        catch (error)
        {
            alert("Error Occured !")
        }

    }


    return (
        <>
            <h2> Login </h2>
            <form onSubmit={handleSubmit}>
                <input type="text" value={username} onChange={ (e) => setUsername(e.target.value)} />   
                <input type="password" value={password} onChange={ (e) => setPassword(e.target.value)} />

                <button type="submit"> Log in</button>
            </form>        
        </>
    )
}

export default Login;