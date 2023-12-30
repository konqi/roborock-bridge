import mqtt, {MqttClient} from "mqtt"
import {useCallback, useEffect, useState} from "react";
import {Device, Path, Position, Routine, VirtualWalls} from "./types.ts";
import CleanupRoutinesModal from "./CleanupRoutinesModal.tsx";
import SvgMap from "./SvgMap.tsx";
import {mock_charger_position, mock_map_data, mock_path, mock_robot_position, mock_virtual_walls} from "./mock-data.ts";

const topics = [
    "mqtt-bridge/home/+/routine/+",
    "mqtt-bridge/home/+/device/+",
    "mqtt-bridge/home/+/device/+/state",
    "mqtt-bridge/home/+/device/+/error",
    "mqtt-bridge/home/+/device/+/battery",
    "mqtt-bridge/home/+/device/+/map",
    "mqtt-bridge/home/+/device/+/virtual_walls",
    "mqtt-bridge/home/+/device/+/path",
    "mqtt-bridge/home/+/device/+/charger_position",
    "mqtt-bridge/home/+/device/+/robot_position",
    "mqtt-bridge/home/+/device/+/goto_path"
]

let mqttClient: MqttClient | null

function App() {
    const [deviceList, setDeviceList] = useState<Device[]>([])
    const [routineList, setRoutineList] = useState<Routine[]>([])
    const [mapData, setMapData] = useState<string>(mock_map_data)
    const [robotPosition, setRobotPosition] = useState<Position>(mock_robot_position)
    const [chargerPosition, setChargerPosition] = useState<Position>(mock_charger_position)
    const [path, setPath] = useState<Path>(mock_path)
    const [virtualWalls, setVirtualWalls] = useState<VirtualWalls>(mock_virtual_walls)
    const [cleanupModalOpen, setCleanupModalOpen] = useState(false)

    useEffect(() => {
        if (!mqttClient) {
            console.log("connecting")
            mqttClient = mqtt.connect("mqtt://localhost:1884")

            console.log("subscribing")
            mqttClient.subscribe(topics, {qos: 0})
            mqttClient.on("message", (topic, message) => {
                if (topic.match(/device\/[^/]*$/)) {
                    const device: Device = JSON.parse(message.toString())
                    setDeviceList((oldList) => [...oldList.filter((deviceInList) => deviceInList.deviceId !== device.deviceId),
                        device])
                }
                const routineMatches = topic.match(/routine\/([^/]*)$/)
                if (routineMatches) {
                    const routine: Routine = JSON.parse(message.toString())
                    setRoutineList((oldList) =>
                        [...oldList.filter((routineInList) => routineInList.id !== routine.id), routine])
                }

                switch (topic.split('/').at(-1)) {
                    case 'map':
                        setMapData(message.toString())
                        break
                    case 'robot_position':
                        setRobotPosition(JSON.parse(message.toString()))
                        break
                    case 'charger_position':
                        setChargerPosition(JSON.parse(message.toString()))
                        break
                    case 'path':
                        setPath(JSON.parse(message.toString()))
                        break
                    case 'virtual_walls':
                        setVirtualWalls(JSON.parse(message.toString()))
                        break
                    case 'goto_path':
                    default:
                        break
                }
            })
        }

        return () => {
            console.log("disconnecting")
            mqttClient?.unsubscribe(topics)
            mqttClient?.end()
            mqttClient = null
        }
    }, [])


    const refresh = useCallback(() => {
        deviceList.forEach(async (device) => {
            console.log(`refreshing ${device.deviceId}`)
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/get`, "", {qos: 0})
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/get`, "map", {qos: 0})
        })
    }, [deviceList]);

    const dock = useCallback(() => {
        deviceList.forEach(async (device) => {
            console.log(`docking ${device.deviceId}`)
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/action`, "home", {qos: 0})
        })
    }, [deviceList])


    return (
        <div className="min-h-full">
            <nav className="bg-gray-800">
                <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                    <div className="flex h-16 items-center justify-between">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <header className="bg-gray-100 shadow-lg">
                                    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                                        <h1 className="text-3xl font-bold tracking-tight text-gray-800">Map</h1>
                                    </div>
                                </header>
                            </div>
                            <div className="md:block">
                                <div className="ml-10 flex items-baseline space-x-4">
                                    <a href="#" onClick={refresh}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >Refresh</a>
                                    <a href="#" onClick={dock}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Dock</a>
                                    <a href="#" onClick={() => setCleanupModalOpen(true)}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Clean</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </nav>

            <main>
                <CleanupRoutinesModal isModalOpen={cleanupModalOpen} closeModal={() => setCleanupModalOpen(false)}
                                      routines={routineList} mqttClient={mqttClient!!}/>
                <div className="mx-auto max-w-7xl py-6 sm:px-6 lg:px-8">
                    <SvgMap imageUrl={mapData} robotPosition={robotPosition} chargerPosition={chargerPosition}
                            path={path} virtualWalls={virtualWalls}/>
                </div>
            </main>
        </div>
    )
}

export default App
