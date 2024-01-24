import { useCallback, useContext, useEffect, useState } from 'react'
import {
    BinaryData,
    ObjectPosition,
    Path,
    Room,
    Routine,
    VirtualWalls,
} from './types.ts'
import CleanupRoutinesModal from './CleanupRoutinesModal.tsx'
import SvgMap from './SvgMap.tsx'
import {
    mock_bitmap_data,
    mock_charger_position,
    mock_map_data,
    mock_path,
    mock_robot_position,
    mock_room_mapping,
    mock_virtual_walls,
} from './mock-data.ts'
import SvgMapClient from './SvgMapClient.tsx'
import { AppContext } from './AppContext.tsx'
import { HomeAndDeviceSelect } from './HomeAndDeviceSelect.tsx'
import { MqttContext } from './MqttContext.tsx'
import { MapModeToggle } from './MapModeToggle.tsx'
import ModeModal from './ModeModal.tsx'

function App() {
    const [routineList, setRoutineList] = useState<Routine[]>([])
    const [mapData, setMapData] = useState<BinaryData>(mock_map_data)
    const [bitmapData, setBitmapData] = useState<BinaryData>(mock_bitmap_data)
    const [robotPosition, setRobotPosition] =
        useState<ObjectPosition>(mock_robot_position)
    const [chargerPosition, setChargerPosition] = useState<ObjectPosition>(
        mock_charger_position
    )
    const [path, setPath] = useState<Path>(mock_path)
    const [virtualWalls, setVirtualWalls] =
        useState<VirtualWalls>(mock_virtual_walls)
    const [cleanupModalOpen, setCleanupModalOpen] = useState(false)
    const [modeModalOpen, setModeModalOpen] = useState(false)
    const [roomList, setRoomList] = useState<Room[]>(mock_room_mapping)

    const { selectedHomeId, selectedDeviceId, useClientMap } =
        useContext(AppContext)

    const { publish, addListener } = useContext(MqttContext)

    useEffect(() => {
        addListener((topic, message) => {
            const routineMatches = topic.match(/routine\/([^/]*)$/)
            if (routineMatches) {
                const routine: Routine = JSON.parse(message.toString())
                setRoutineList((oldList) => [
                    ...oldList.filter(
                        (routineInList) => routineInList.id !== routine.id
                    ),
                    routine,
                ])
            }

            switch (topic.split('/').at(-1)) {
                case 'map':
                    setMapData(JSON.parse(message.toString()))
                    break
                case 'bitmap_data':
                    setBitmapData(JSON.parse(message.toString()))
                    break
                case 'robot_position':
                    console.log(message.toString())
                    setRobotPosition(JSON.parse(message.toString()))
                    break
                case 'charger_position':
                    console.log(message.toString())
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
    }, [])

    const refresh = useCallback(async () => {
        console.log(`refreshing ${selectedDeviceId}`)
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/get`,
            'state',
            { qos: 0 }
        )
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/get`,
            'map',
            { qos: 0 }
        )
    }, [selectedHomeId, selectedDeviceId, publish])

    const dock = useCallback(async () => {
        console.log(`docking ${selectedDeviceId}`)
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/action`,
            'home',
            { qos: 0 }
        )
    }, [selectedHomeId, selectedDeviceId, publish])

    const pause = useCallback(async () => {
        console.log(`pausing ${selectedDeviceId}`)
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/action`,
            'pause',
            { qos: 0 }
        )
    }, [selectedHomeId, selectedDeviceId, publish])

    const resume = useCallback(async () => {
        console.log(`resuming ${selectedDeviceId}`)
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/action`,
            'start',
            { qos: 0 }
        )
    }, [selectedHomeId, selectedDeviceId, publish])

    const cleanSegments = useCallback(
        async (segments: number[]) => {
            const params = { action: 'segments', segments }

            console.log(
                `cleaning segments [${params.segments.join(', ')}] for ${selectedDeviceId}`
            )
            await publish(
                `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/action`,
                JSON.stringify(params),
                { qos: 0 }
            )
        },
        [selectedHomeId, selectedDeviceId, publish]
    )

    return (
        <div className="min-h-full">
            <nav className="bg-gray-800">
                <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                    <div className="flex h-16 items-center justify-between">
                        <div className="flex items-center">
                            <div className="flex-shrink-0">
                                <header className="bg-gray-100 shadow-lg">
                                    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                                        <h1 className="text-3xl font-bold tracking-tight text-gray-800">
                                            Map
                                        </h1>
                                    </div>
                                </header>
                            </div>
                            <div className="md:block">
                                <div className="ml-10 flex items-baseline space-x-4">
                                    <a
                                        href="#"
                                        onClick={refresh}
                                        className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Refresh
                                    </a>
                                    <a
                                        href="#"
                                        onClick={() =>
                                            setCleanupModalOpen(true)
                                        }
                                        className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Routine
                                    </a>
                                    <a href="#"
                                       onClick={() =>
                                           setModeModalOpen(true)
                                       }
                                       className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Mode
                                    </a>
                                    <a
                                        href="#"
                                        onClick={dock}
                                        className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Dock
                                    </a>
                                    <a
                                        href="#"
                                        onClick={pause}
                                        className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Pause
                                    </a>
                                    <a
                                        href="#"
                                        onClick={resume}
                                        className="text-gray-300 hover:bg-gray-700 hover:text-white rounded-md px-3 py-2 text-sm font-medium"
                                    >
                                        Resume
                                    </a>
                                    <HomeAndDeviceSelect />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </nav>

            <main>
                <CleanupRoutinesModal
                    isModalOpen={cleanupModalOpen}
                    closeModal={() => setCleanupModalOpen(false)}
                    routines={routineList}
                />
                <ModeModal isModalOpen={modeModalOpen} closeModal={() => setModeModalOpen(false)}/>
                <div className="mx-auto max-w-7xl py-6 sm:px-6 lg:px-8">
                    <MapModeToggle />
                    {!useClientMap && (
                        <SvgMap
                            imageUrl={mapData}
                            robotPosition={robotPosition}
                            chargerPosition={chargerPosition}
                            path={path}
                            virtualWalls={virtualWalls}
                        />
                    )}
                    {useClientMap && (
                        <SvgMapClient
                            bitmapData={bitmapData}
                            robotPosition={robotPosition}
                            chargerPosition={chargerPosition}
                            path={path}
                            virtualWalls={virtualWalls}
                            roomList={roomList}
                            cleanSegments={cleanSegments}
                        />
                    )}
                </div>
            </main>
        </div>
    )
}

export default App
