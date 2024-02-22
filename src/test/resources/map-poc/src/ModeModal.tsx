import Modal from './Modal.tsx'
import { useCallback, useContext, useEffect, useState } from 'react'
import { MqttContext } from './MqttContext.tsx'
import { AppContext } from './AppContext.tsx'
import { Options } from './types.ts'

interface ModeModalProps {
    isModalOpen: boolean
    closeModal: () => void
}

function ModeModal({ isModalOpen, closeModal }: ModeModalProps) {
    const { selectedHomeId, selectedDeviceId } = useContext(AppContext)
    const { publish, addListener, removeListener } = useContext(MqttContext)
    const [fanPowerOptions, setFanPowerOptions] = useState<Options>({})
    const [mopModeOptions, setMopModeOptions] = useState<Options>({})
    const [waterBoxModeOptions, setWaterBoxModeOptions] = useState<Options>({})

    const [selectedFanPower, setSelectedFanPower] = useState<number>()
    const [selectedMopMode, setSelectedMopMode] = useState<number>()
    const [selectedWaterBoxMode, setSelectedWaterBoxMode] = useState<number>()

    useEffect(() => {
        if(!selectedHomeId || !selectedDeviceId) {
            return
        }

        const listener = (topic: string, message: Buffer) => {
            if (topic.endsWith('options') && message.length > 0) {
                if (topic.endsWith('fan_power/options')) {
                    setFanPowerOptions(JSON.parse(message.toString()))
                } else if (topic.endsWith('mop_mode/options')) {
                    setMopModeOptions(JSON.parse(message.toString()))
                } else if (topic.endsWith('water_box_mode/options')) {
                    setWaterBoxModeOptions(JSON.parse(message.toString()))
                }
            }
        }

        addListener(listener)

        const worker = async () => {
            await publish(
                `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/fan_power/options`,
                '',
                { qos: 0 }
            )
            await publish(
                `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/mop_mode/options`,
                '',
                { qos: 0 }
            )
            await publish(
                `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/water_box_mode/options`,
                '',
                { qos: 0 }
            )
        }

        worker()

        return () => {
            removeListener(listener)
        }
    }, [selectedHomeId, selectedDeviceId, addListener, removeListener, publish])

    useEffect(() => {
        if (!selectedFanPower && Object.keys(fanPowerOptions).length > 0) {
            setSelectedFanPower(Object.values(fanPowerOptions)[0])
        }
        if (!selectedMopMode && Object.keys(mopModeOptions).length > 0) {
            setSelectedMopMode(Object.values(mopModeOptions)[0])
        }
        if (
            !selectedWaterBoxMode &&
            Object.keys(waterBoxModeOptions).length > 0
        ) {
            setSelectedWaterBoxMode(Object.values(waterBoxModeOptions)[0])
        }
    }, [fanPowerOptions, mopModeOptions, selectedFanPower, selectedMopMode, selectedWaterBoxMode, waterBoxModeOptions])

    const setValues = useCallback(async () => {
        await publish(
            `mqtt-bridge/home/${selectedHomeId}/device/${selectedDeviceId}/action`,
            JSON.stringify({
                action: 'clean_mode',
                fan_power: selectedFanPower,
                mop_mode: selectedMopMode,
                water_box_mode: selectedWaterBoxMode,
            })
        )
        closeModal()
    }, [closeModal, publish, selectedDeviceId, selectedFanPower, selectedHomeId, selectedMopMode, selectedWaterBoxMode])

    return (
        <Modal
            open={isModalOpen}
            title="Clean Mode"
            body={
                <>
                    <label
                        htmlFor="fan_power"
                        className="block text-sm font-medium leading-6 text-gray-900"
                    >
                        Fan Power
                    </label>
                    <div className="mt-2">
                        <select
                            id="fan_power"
                            name="fan_power"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
                            onChange={(event) =>
                                setSelectedFanPower(
                                    parseInt(event.target.value)
                                )
                            }
                            value={selectedFanPower}
                        >
                            {Object.keys(fanPowerOptions).map((key) => (
                                <option key={key} value={fanPowerOptions[key]}>
                                    {key}
                                </option>
                            ))}
                        </select>
                    </div>
                    <label
                        htmlFor="mop_mode"
                        className="block text-sm font-medium leading-6 text-gray-900"
                    >
                        Mop Mode
                    </label>
                    <div className="mt-2">
                        <select
                            id="mop_mode"
                            name="mop_mode"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
                            onChange={(event) =>
                                setSelectedMopMode(parseInt(event.target.value))
                            }
                            value={selectedMopMode}
                        >
                            {Object.keys(mopModeOptions).map((key) => (
                                <option key={key} value={mopModeOptions[key]}>
                                    {key}
                                </option>
                            ))}
                        </select>
                    </div>
                    <label
                        htmlFor="water_box_mode"
                        className="block text-sm font-medium leading-6 text-gray-900"
                    >
                        Water Box Mode
                    </label>
                    <div className="mt-2">
                        <select
                            id="water_box_mode"
                            name="water_box_mode"
                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
                            onChange={(event) =>
                                setSelectedWaterBoxMode(
                                    parseInt(event.target.value)
                                )
                            }
                            value={selectedWaterBoxMode}
                        >
                            {Object.keys(waterBoxModeOptions).map((key) => (
                                <option
                                    key={key}
                                    value={waterBoxModeOptions[key]}
                                >
                                    {key}
                                </option>
                            ))}
                        </select>
                    </div>
                </>
            }
            actions={
                <>
                    <button
                        type="button"
                        onClick={setValues}
                        className="inline-flex w-full justify-center rounded-md bg-red-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 sm:ml-3 sm:w-auto"
                    >
                        Set
                    </button>
                    <button
                        type="button"
                        onClick={closeModal}
                        className="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto"
                    >
                        Cancel
                    </button>
                </>
            }
        />
    )
}

export default ModeModal
