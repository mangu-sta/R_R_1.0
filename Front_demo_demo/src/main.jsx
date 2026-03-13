import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import './index.css'
import App from './App.jsx'
import GameMain from './components/GameMain.jsx';
import Tutorial from './components/Tutorial.jsx';

createRoot(document.getElementById('root')).render(
  <>
    <ToastContainer
      position="top-right"
      autoClose={2000}
      hideProgressBar={false}
      newestOnTop
      closeOnClick
      rtl={false}
      pauseOnFocusLoss={false} // 창 전환 시 멈추지 않게
      pauseOnHover={false}     // 마우스 hover 시 멈추지 않게
      draggable
      theme="dark"
      className="custom-toast-container"
      toastClassName="custom-toast"
      bodyClassName="custom-toast-body"
      progressClassName="custom-toast-progress"
    />
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App step="SignIn"/>} />
        <Route path="/signin" element={<App step="SignIn"/>} />
        <Route path="/signup" element={<App step="SignUp"/>} />
        <Route path="/createcharacter" element={<App step="CreateCharacter"/>} />
        <Route path="/lobby" element={<App step="lobby"/>} />
        
        <Route path="/tutorial" element={<Tutorial />} />
        <Route path="/game" element={<GameMain />} />

      </Routes>
    </BrowserRouter>
  </>
  
)
