import Modal from "./Modal.tsx";
import { useCallback, useContext, useState } from 'react'
import {Routine} from "./types.ts";
import { MqttContext } from './MqttContext.tsx'

interface CleanupRoutinesModalProps {
    isModalOpen: boolean
    closeModal: () => void
    routines: Routine[]
}

function CleanupRoutinesModal({isModalOpen, closeModal, routines}:CleanupRoutinesModalProps) {
    const [cleanupRoutine, setCleanupRoutine] = useState<Routine | null>(null)

    const {publish} = useContext(MqttContext)

    const startCleanup = useCallback(async () => {
        await publish(`mqtt-bridge/home/${cleanupRoutine?.homeId}/routine/${cleanupRoutine?.id}/action`, "", {qos: 0})
        closeModal()
    }, [cleanupRoutine])

    return <Modal open={isModalOpen} title="Start Cleanup" body={<>
        <label htmlFor="routine"
               className="block text-sm font-medium leading-6 text-gray-900">Select Routine</label>
        <div className="mt-2">
            <select id="routine" value={cleanupRoutine?.id} onChange={(e) => {
                setCleanupRoutine(routines.find((routine) => routine.id === parseInt(e.target.value))!!)
            }} name="routine" autoComplete="country-name"
                    className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6">
                {routines.map(routine => <option key={routine.id} value={routine.id}>{routine.name}</option>)}
            </select>
        </div>
    </>} actions={<>
        <button type="button" onClick={startCleanup}
                className="inline-flex w-full justify-center rounded-md bg-red-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 sm:ml-3 sm:w-auto">Start
        </button>
        <button type="button" onClick={closeModal}
                className="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto">Cancel
        </button>
    </>
    }/>
}

export default CleanupRoutinesModal