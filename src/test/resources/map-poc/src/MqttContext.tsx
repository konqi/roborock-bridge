import {
    createContext,
    ReactNode,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react'
import mqtt, { MqttClient } from 'mqtt'
import { Device } from './types.ts'
import { AppContext } from './AppContext.tsx'
import { IClientPublishOptions } from 'mqtt/lib/client'

const BROKER_URL = 'mqtt://localhost:9001'
const topics = [
    'mqtt-bridge/home/+/routine/+',
    'mqtt-bridge/home/+/device/+',
    'mqtt-bridge/home/+/rooms',
    'mqtt-bridge/home/+/device/+/state',
    'mqtt-bridge/home/+/device/+/error',
    'mqtt-bridge/home/+/device/+/battery',
    'mqtt-bridge/home/+/device/+/map',
    'mqtt-bridge/home/+/device/+/bitmap_data',
    'mqtt-bridge/home/+/device/+/virtual_walls',
    'mqtt-bridge/home/+/device/+/path',
    'mqtt-bridge/home/+/device/+/charger_position',
    'mqtt-bridge/home/+/device/+/robot_position',
    'mqtt-bridge/home/+/device/+/goto_path',
    'mqtt-bridge/home/+/device/+/fan_power',
    'mqtt-bridge/home/+/device/+/mop_mode',
    'mqtt-bridge/home/+/device/+/water_box_mode',
    'mqtt-bridge/home/+/device/+/fan_power/options',
    'mqtt-bridge/home/+/device/+/mop_mode/options',
    'mqtt-bridge/home/+/device/+/water_box_mode/options',
]

type PublishFn = (
    topic: string,
    message: string | Buffer,
    opts?: IClientPublishOptions
) => Promise<void>

interface MqttContext {
    publish: PublishFn
    addListener: (listener: MessageListener) => void
    removeListener: (listener: MessageListener) => void
}

type MessageListener = (topic: string, message: Buffer) => void

const mqttContextDefault: MqttContext = {
    publish: () => {
        throw new Error('missing implementation')
    },
    addListener: () => {
        throw new Error('missing implementation')
    },
    removeListener: () => {
        throw new Error('missing implementation')
    },
}

export const MqttContext = createContext<MqttContext>(mqttContextDefault)

export function MqttProvider({ children }: { children: ReactNode }) {
    const mqttClientRef = useRef<MqttClient | null>(null)

    const [subscribedListeners, setSubscribedListeners] = useState<
        MessageListener[]
    >([])

    const { deviceList, updateDeviceList, homeList, updateHomeList } =
        useContext(AppContext)

    const addListener = useCallback(
        (listener: MessageListener) =>
            setSubscribedListeners((listeners) => {
                const listenersExNewListener = listeners.filter(
                    (curr) => curr !== listener
                )
                return [...listenersExNewListener, listener]
            }),
        []
    )
    const removeListener = useCallback(
        (listener: MessageListener) =>
            setSubscribedListeners((listeners) =>
                listeners.filter((curr) => curr !== listener)
            ),
        []
    )

    useEffect(() => {
        if (!mqttClientRef.current) {
            console.log('connecting')
            mqttClientRef.current = mqtt.connect(BROKER_URL)

            console.log('subscribing')
            mqttClientRef.current.subscribe(topics, { qos: 0 })
        }

        return () => {
            console.log('disconnecting')
            mqttClientRef.current?.unsubscribe(topics)
            mqttClientRef.current?.end()
            mqttClientRef.current = null
        }
    }, [])

    useEffect(() => {
        if (mqttClientRef.current) {
            const callback = (topic: string, message: Buffer) => {
                if (topic.match(/device\/[^/]*$/)) {
                    const device: Device = JSON.parse(message.toString())
                    updateDeviceList([
                        ...deviceList.filter(
                            (deviceInList) =>
                                deviceInList.deviceId !== device.deviceId
                        ),
                        device,
                    ])
                }
                const [_, homeId] = topic.match(/home\/([^/]*)/) ?? []
                if (homeId.trim()) {
                    updateHomeList([
                        ...homeList.filter(
                            (homeInList) => homeInList !== homeId
                        ),
                        homeId,
                    ])
                }
                subscribedListeners.forEach((cb) => cb(topic, message))
            }

            // remove old message listener
            for (const oldCallback of mqttClientRef.current.listeners(
                'message'
            )) {
                mqttClientRef.current.off('message', oldCallback)
            }
            // add new message listener
            mqttClientRef.current.on('message', callback)
        }
    }, [subscribedListeners])

    const publish: PublishFn = useCallback(
        async (topic, message, opts) => {
            if (mqttClientRef.current) {
                await mqttClientRef.current.publishAsync(topic, message, opts)
            } else {
                console.log('publish not set')
            }
        },
        [mqttClientRef]
    )

    const value = useMemo(() => ({ publish, addListener, removeListener }), [])

    return <MqttContext.Provider value={value}>{children}</MqttContext.Provider>
}
