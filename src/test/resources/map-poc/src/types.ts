export interface Position {
    x: number,
    y: number,
    a: number
}

export type VirtualWall = [{x:number,y:number},{x:number,y:number}]
export type VirtualWalls = VirtualWall[]

export interface Device {
    homeId: number
    deviceId: string
    deviceKey: string
    name: string
    productName: string
    model: string
    firmwareVersion: string
    serialNumber: string
}

export interface Routine {
    homeId: number
    id: number
    name: string
}

export type Path = Array<{ x: number, y: number }>