import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { AppContextProvider } from './AppContext.tsx'
import { MqttProvider } from './MqttContext.tsx'

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <AppContextProvider>
            <MqttProvider>
                <App />
            </MqttProvider>
        </AppContextProvider>
    </React.StrictMode>
)
