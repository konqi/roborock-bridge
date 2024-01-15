import mqtt, {MqttClient} from "mqtt"
import {useCallback, useEffect, useState} from "react";
import {BinaryData, Device, ObjectPosition, Path, Room, Routine, VirtualWalls} from "./types.ts";
import CleanupRoutinesModal from "./CleanupRoutinesModal.tsx";
import SvgMap from "./SvgMap.tsx";
import {
    mock_bitmap_data,
    mock_charger_position,
    mock_map_data,
    mock_path,
    mock_robot_position, mock_room_mapping,
    mock_virtual_walls
} from "./mock-data.ts";
import SvgMapClient from "./SvgMapClient.tsx";

const BROKER_URL = "mqtt://localhost:9001"
const topics = [
    "mqtt-bridge/home/+/routine/+",
    "mqtt-bridge/home/+/device/+",
    "mqtt-bridge/home/+/rooms",
    "mqtt-bridge/home/+/device/+/state",
    "mqtt-bridge/home/+/device/+/error",
    "mqtt-bridge/home/+/device/+/battery",
    "mqtt-bridge/home/+/device/+/map",
    "mqtt-bridge/home/+/device/+/bitmap_data",
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
    const [mapData, setMapData] = useState<BinaryData>(mock_map_data)
    const [bitmapData, setBitmapData] = useState<BinaryData>(mock_bitmap_data)
    const [robotPosition, setRobotPosition] = useState<ObjectPosition>(mock_robot_position)
    const [chargerPosition, setChargerPosition] = useState<ObjectPosition>(mock_charger_position)
    const [path, setPath] = useState<Path>(mock_path)
    const [virtualWalls, setVirtualWalls] = useState<VirtualWalls>(mock_virtual_walls)
    const [cleanupModalOpen, setCleanupModalOpen] = useState(false)
    const [roomList, setRoomList] = useState<Room[]>(mock_room_mapping)
    const [useClientMap, setUseClientMap] = useState(false)

    useEffect(() => {
        if (!mqttClient) {
            console.log("connecting")
            mqttClient = mqtt.connect(BROKER_URL)

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
                        setMapData(JSON.parse(message.toString()))
                        break
                    case 'bitmap_data':
                        setBitmapData(JSON.parse(message.toString()))
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
                    case 'rooms':
                        setRoomList(JSON.parse(message.toString()))
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

    const pause = useCallback(() => {
        deviceList.forEach(async (device) => {
            console.log(`pausing ${device.deviceId}`)
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/action`, "pause", {qos: 0})
        })
    }, [deviceList])

    const resume = useCallback(() => {
        deviceList.forEach(async (device) => {
            console.log(`resuming ${device.deviceId}`)
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/action`, "start", {qos: 0})
        })
    }, [deviceList])

    const cleanSegments = useCallback((segments: number[]) => {
        const params = {action: "segments", segments}

        deviceList.forEach(async (device) => {
            console.log(`cleaning segments [${params.segments.join(", ")}] for ${device.deviceId}`)
            await mqttClient?.publishAsync(`mqtt-bridge/home/${device.homeId}/device/${device.deviceId}/action`, JSON.stringify(params), {qos: 0})
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
                                    <a href="#" onClick={() => setCleanupModalOpen(true)}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Routine</a>
                                    <a href="#" onClick={dock}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Dock</a>
                                    <a href="#" onClick={pause}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Pause</a>
                                    <a href="#" onClick={resume}
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium">Resume</a>
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
                    <label htmlFor="toggleClientMap" className="flex items-center cursor-pointer">
                        <div className="relative">
                            <input type="checkbox" id="toggleClientMap" className="sr-only" onChange={() => {
                                setUseClientMap(!useClientMap)
                            }}/>
                            <div className="block bg-gray-600 w-14 h-8 rounded-full"></div>
                            <div className="dot absolute left-1 top-1 bg-white w-6 h-6 rounded-full transition"
                                 style={useClientMap ? {
                                     transform: 'translateX(100%)',
                                     backgroundColor: '#48bb78'
                                 } : {}}></div>
                        </div>
                        <div className="ml-3 text-gray-700 font-medium">
                            Use client generated map
                        </div>
                    </label>
                    {!useClientMap &&
                        <SvgMap imageUrl={mapData}
                                robotPosition={robotPosition}
                                chargerPosition={chargerPosition}
                                path={path}
                                virtualWalls={virtualWalls}/>
                    }
                    {useClientMap &&
                        <SvgMapClient bitmapData={bitmapData}
                                      robotPosition={robotPosition}
                                      chargerPosition={chargerPosition}
                                      path={path}
                                      virtualWalls={virtualWalls}
                                      roomList={roomList}
                                      cleanSegments={cleanSegments}/>
                    }
                </div>
            </main>
        </div>
    )
}

export default App
