import { useContext } from 'react'
import { AppContext } from './AppContext.tsx'

export function MapModeToggle() {
    const {useClientMap, setUseClientMap} = useContext(AppContext)
    return <label
        htmlFor="toggleClientMap"
        className="flex items-center cursor-pointer"
    >
        <div className="relative">
            <input
                type="checkbox"
                id="toggleClientMap"
                className="sr-only"
                onChange={() => {
                    setUseClientMap(!useClientMap)
                }}
            />
            <div className="block bg-gray-600 w-14 h-8 rounded-full"></div>
            <div
                className="dot absolute left-1 top-1 bg-white w-6 h-6 rounded-full transition"
                style={
                    useClientMap
                        ? {
                            transform: 'translateX(100%)',
                            backgroundColor: '#48bb78',
                        }
                        : {}
                }
            ></div>
        </div>
        <div className="ml-3 text-gray-700 font-medium">
            Use client generated map
        </div>
    </label>
}