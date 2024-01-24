import { createContext, ReactNode, useState } from 'react'
import { Device } from './types.ts'

interface AppContext {
    homeList: string[]
    deviceList: Device[]
    selectedHomeId: string | undefined
    selectedDeviceId: string | undefined
    updateHomeList: (newHomeList: string[]) => void
    updateDeviceList: (newDeviceList: Device[]) => void
    setSelectedHome: (homeId: string) => void
    setSelectedDevice: (deviceId: string) => void
    useClientMap: boolean,
    setUseClientMap: (use: boolean) => void
}

const defaultContextValues: AppContext = {
    deviceList: [],
    homeList: [],
    selectedDeviceId: undefined,
    selectedHomeId: undefined,
    useClientMap: false,
    setUseClientMap: () => {
        throw new Error('missing implementation')
    },
    updateDeviceList: () => {
        throw new Error('missing implementation')
    },
    updateHomeList: () => {
        throw new Error('missing implementation')
    },
    setSelectedDevice: () => {
        throw new Error('missing implementation')
    },
    setSelectedHome: () => {
        throw new Error('missing implementation')
    },
}

export const AppContext = createContext<AppContext>(defaultContextValues)

export function AppContextProvider({ children }: { children: ReactNode }) {
    const [context, setContext] = useState<AppContext>(defaultContextValues)

    const updateHomeList = (newHomeList: string[]) => {
        setContext((ctx) => ({
            ...ctx,
            homeList: newHomeList,
            selectedHomeId: ctx.selectedHomeId ?? newHomeList[0],
        }))
    }
    const updateDeviceList = (newDeviceList: Device[]) => {
        setContext((ctx) => ({
            ...ctx,
            deviceList: newDeviceList,
            selectedDeviceId: ctx.selectedDeviceId ?? newDeviceList[0].deviceId,
        }))
    }
    const setSelectedDevice = (deviceId: string) =>
        setContext((ctx) => ({ ...ctx, selectedDeviceId: deviceId }))
    const setSelectedHome = (homeId: string) =>
        setContext((ctx) => ({ ...ctx, selectedHomeId: homeId }))
    const setUseClientMap = (useClientMap: boolean) => setContext((ctx) => ({...ctx, useClientMap}))

    return (
        <AppContext.Provider
            value={{
                ...context,
                setUseClientMap,
                updateDeviceList,
                updateHomeList,
                setSelectedHome,
                setSelectedDevice,
            }}
        >
            {children}
        </AppContext.Provider>
    )
}
