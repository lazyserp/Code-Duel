import { useState } from 'react'
import { BrowserRouter } from "react-router-dom"
import { Routes } from 'react-router-dom'
import { Route } from 'react-router-dom'
import Login from './components/Login'
import Register from './components/Register'
import Lobby from './components/Lobby'
import Arena from './components/Arena'
import Leaderboard from './components/Leaderboard'
import Landing from './components/Landing'
import './App.css'
import ProtectedRoute from './components/ProtectedRoute'
function App() {

  return (
    <BrowserRouter>
     <Routes>
      <Route path='/' element={<Landing/>} />
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register/>} />

      <Route element={<ProtectedRoute/>}>
        <Route path='/lobby' element={<Lobby/>} />
        <Route path='/arena' element={<Arena/>} />
        <Route path='/leaderboard' element={<Leaderboard/>} />
      </Route>
      
     </Routes>
     </BrowserRouter>
  )
}

export default App
