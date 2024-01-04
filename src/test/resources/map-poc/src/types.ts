export interface Coordinate {
    x: number,
    y: number,
}

export interface Position extends Coordinate {
    a: number
}

export interface Room {
    roomId: number
    name: string
    mqttRoomId: number
    position?: Coordinate
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

type RGB = `rgb(${number}, ${number}, ${number})`;
type RGBA = `rgba(${number}, ${number}, ${number}, ${number})`;

export type Color = RGB | RGBA;


export enum PixelType {
    OUTSIDE,
    OBSTACLE_ROOM_1,
    OBSTACLE_ROOM_2,
    OBSTACLE_ROOM_3,
    OBSTACLE_ROOM_4,
    OBSTACLE_ROOM_5,
    OBSTACLE_ROOM_6,
    OBSTACLE_ROOM_7,
    OBSTACLE_ROOM_8,
    OBSTACLE_ROOM_9,
    OBSTACLE_ROOM_10,
    OBSTACLE_ROOM_11,
    OBSTACLE_ROOM_12,
    OBSTACLE_ROOM_13,
    OBSTACLE_ROOM_14,
    OBSTACLE_ROOM_15,
    OBSTACLE_ROOM_16,
    OBSTACLE_ROOM_17,
    OBSTACLE_ROOM_18,
    OBSTACLE_ROOM_19,
    OBSTACLE_ROOM_20,
    OBSTACLE_ROOM_21,
    OBSTACLE_ROOM_22,
    OBSTACLE_ROOM_23,
    OBSTACLE_ROOM_24,
    OBSTACLE_ROOM_25,
    OBSTACLE_ROOM_26,
    OBSTACLE_ROOM_27,
    OBSTACLE_ROOM_28,
    OBSTACLE_ROOM_29,
    OBSTACLE_ROOM_30,
    OBSTACLE_ROOM_31,
    OBSTACLE_ROOM_0,
    INSIDE,
    WALL,
    SCAN,
    OBSTACLE_WALL,
    OBSTACLE_WALL_V2,
    OTHER
}