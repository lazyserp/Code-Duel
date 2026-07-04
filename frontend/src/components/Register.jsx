import { useState } from "react";
import  axios from "axios";



function Register()
{
    const [username , setUsername] = useState("")
    const [email , setEmail] = useState("")
    const [password, setPassword] = useState("")

    async function handleSubmit(e)
    {
        e.preventDefault()
        try{
            await axios.post("http://localhost:8080/api/auth/register", {username,email,password})
            alert("Registration Successfull !")

        }
        catch(error) {
            alert("Registration Failed !" + error.message)
        }
    }

    return (
        <>
            <h2> Regsiter </h2>
            <form onSubmit={handleSubmit}>
                <input type="text" placeholder="Username" value={username} onChange={ (e) => setUsername(e.target.value)}/>
                <input type="password" placeholder="Password" value={password} onChange={ (e) => setPassword(e.target.value) }/>
                <input type="email" placeholder="email" value={email} onChange={ (e) => setEmail(e.target.value)}/>

                <button type="submit" > Register</button>
            </form>
        </>
    )
}


export default Register;