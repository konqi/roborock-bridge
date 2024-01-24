import { useContext } from 'react'
import { AppContext } from './AppContext.tsx'

export function HomeAndDeviceSelect() {
    const { homeList, deviceList, setSelectedHome, setSelectedDevice, selectedHomeId, selectedDeviceId } =
        useContext(AppContext)

    return (
        <>
            <label
                htmlFor="home"
    className="block text-sm font-medium leading-6 text-gray-300"
        >
        Home
        </label>
        <select
    id="home"
    name="home"
    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
    onChange={(event) => {
        setSelectedHome(event.target.value)
    }}
    value={selectedHomeId}
>
    {homeList.map((homeId) => (
        <option key={homeId} value={homeId}>{homeId}</option>
    ))}
    </select>
    <label
    htmlFor="device"
    className="block text-sm font-medium leading-6 text-gray-300"
        >
        Device
        </label>
        <select
    id="device"
    name="device"
    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
    onChange={(event) => {
        setSelectedDevice(event.target.value)
    }}
    value={selectedDeviceId}
>
    {deviceList.map((device) => (
        <option key={device.deviceId} value={device.deviceId}>{device.deviceId}</option>
    ))}
    </select>
    </>
)
}