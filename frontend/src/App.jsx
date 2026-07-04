import { useState } from 'react'
import { BrowserRouter } from "react-router-dom"
import { Routes } from 'react-router-dom'
import { Route } from 'react-router-dom'
import Login from './components/Login'
import Register from './components/Register'

import './App.css'

function App() {

  return (
    <BrowserRouter>
     <Routes>
      <Route path='/' element={<Login/>} />
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register/>} />
     </Routes>
     </BrowserRouter>
  )
}

export default App
