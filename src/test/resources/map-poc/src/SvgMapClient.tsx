import {BinaryData, Color, Coordinate, ObjectPosition, Path, PixelType, Room, VirtualWalls} from "./types.ts"
import {useCallback, useEffect, useState} from "react"
import "./SvgMapClient.css"

const RR_ORANGE: Color = "rgb(255, 148, 120)"
const RR_YELLOW: Color = "rgb(255, 207, 78)"
const RR_GREEN: Color = "rgb(43, 205, 187)"
const RR_BLUE: Color = "rgb(130, 190, 255)"
const RR_GRAY: Color = "rgb(109, 110, 112)"

async function decompressImageData(data: string) {
    const blob = await (await fetch(`data:application/octet-stream;base64,${data}`)).blob()
    const reader = blob.stream().pipeThrough<Uint8Array>(
        new DecompressionStream('deflate')
    ).getReader()

    let done = false
    const chunks = [] as Uint8Array[]
    do {
        const chunk = await reader.read()
        done = chunk.done

        if (chunk.value) {
            chunks.push(chunk.value)
        }
    } while (!done)

    const output = new Uint8Array(chunks.reduce((acc, curr) => acc + curr.length, 0))
    chunks.reduce((acc, curr) => {
        output.set(curr, acc)
        return acc + curr.length
    }, 0)

    return output
}

class Bitmap {
    private canvas = document.createElement("canvas")
    private context = this.canvas.getContext("2d") as CanvasRenderingContext2D
    private data: Uint8Array[] = []

    readonly layers = new Set<number>()

    neighbors: Record<number, Set<number>> = {}
    rooms: Record<number, { start: Coordinate, end: Coordinate }> = {}

    static isRoom(value: number) {
        return value > 0 && value < 32
    }

    constructor(data: Uint8Array, dimensions: { width: number, height: number }) {
        this.context.canvas.width = dimensions.width
        this.context.canvas.height = dimensions.height

        for (let y = 0; y < dimensions.height; y++) {
            const slice = data.slice(y * dimensions.width, y * dimensions.width + dimensions.width)
            this.data.push(slice)
            slice.forEach((value, x) => {
                this.findNeighbors(x, y)
                this.layers.add(value)
                this.updateRoomDimensions(value, [x, y]);
            })
        }
    }

    private updateRoomDimensions(value: number, [x, y]: Coordinate) {
        if (Bitmap.isRoom(value)) {
            const roomDimensions = this.rooms[value] ?? {
                start: [this.canvas.width, this.canvas.height],
                end: [0, 0]
            }
            this.rooms[value] = {
                start: [
                    x < roomDimensions.start[0] ? x : roomDimensions.start[0],
                    y < roomDimensions.start[1] ? y : roomDimensions.start[1]
                ],
                end: [
                    x > roomDimensions.end[0] ? x : roomDimensions.end[0],
                    y > roomDimensions.end[1] ? y : roomDimensions.end[1]
                ]
            }
        }
    }

    private setNeighbor(roomA: number, roomB: number) {
        if (!this.neighbors[roomA]) {
            this.neighbors[roomA] = new Set<number>()
        }
        if (!this.neighbors[roomB]) {
            this.neighbors[roomB] = new Set<number>()
        }
        this.neighbors[roomA].add(roomB)
        this.neighbors[roomB].add(roomA)
    }

    private findNeighbors(x: number, y: number) {
        const value = this.data[y][x]
        if (Bitmap.isRoom(value)) {
            if (x > 0) {
                const xBefore = this.data[y][x - 1]
                if (Bitmap.isRoom(xBefore) && xBefore !== value) {
                    this.setNeighbor(value, xBefore)
                }
            }
            if (y > 0) {
                const yBefore = this.data[y - 1][x]
                if (Bitmap.isRoom(yBefore) && yBefore !== value) {
                    this.setNeighbor(value, yBefore)
                }
            }
        }
    }

    getDynamicPalette(...colors: Color[]): Record<number, Color> {
        const roomsLayers = [...this.layers].filter(Bitmap.isRoom)
        return roomsLayers.reduce<Record<number, Color>>((acc, curr) => {
            const colorsUsedByNeighbors = [...this.neighbors[curr]].map(neighbor => acc[neighbor])
            const availableColors = colors.filter(color => !colorsUsedByNeighbors.includes(color))

            let pickedColor = availableColors.length > 0 ?
                availableColors[curr % availableColors.length] :
                colors[curr % colors.length]

            return {...acc, [curr]: pickedColor}

        }, {})
    }

    get(value: number, color: Color) {
        const image = this.context.createImageData(this.canvas.width, this.canvas.height)
        const [r, g, b, a] = color.match(/([0-9]*)/g)
            ?.filter(capture => !!capture)
            .map(capture => parseInt(capture)) ?? []

        this.data.forEach((row, y) => row.forEach(((pixel, x) => {
            if (pixel === value) {

                // R
                image.data[y * this.canvas.width * 4 + x * 4] = r
                // G
                image.data[y * this.canvas.width * 4 + x * 4 + 1] = g
                // B
                image.data[y * this.canvas.width * 4 + x * 4 + 2] = b
                // A
                image.data[y * this.canvas.width * 4 + x * 4 + 3] = a ?? 255
            }
        })))

        this.context.clearRect(0, 0, this.canvas.width, this.canvas.height)
        this.context.putImageData(image, 0, 0)
        return this.canvas.toDataURL("image/webp", 1)
    }
}


interface SvgMapClientProps {
    bitmapData: BinaryData
    roomList: Room[]
    robotPosition: ObjectPosition
    chargerPosition: ObjectPosition
    path: Path
    virtualWalls: VirtualWalls
    cleanSegments: (segments: number[]) => void
}

function SvgMapClient({
                          chargerPosition,
                          robotPosition,
                          path,
                          virtualWalls,
                          bitmapData,
                          roomList,
                          cleanSegments: cleanSegmentsCallback
                      }: SvgMapClientProps) {
    const [dimensions, setDimensions] = useState({width: 0, height: 0})
    const [imageMap, setImageMap] = useState<Record<number, string>>({})
    const [roomLabels, setRoomLabels] = useState<Room[]>([])
    const [selectedRooms, setSelectedRooms] = useState<number[]>([])

    const cleanSegments = useCallback(() => {
        cleanSegmentsCallback(selectedRooms)
        setSelectedRooms([])
    }, [selectedRooms, cleanSegmentsCallback]);

    useEffect(() => {
        const width = bitmapData.meta.dimensions[0]
        const height = bitmapData.meta.dimensions[1]
        if (width && height) {
            const dimensions = {width: width, height: height}
            setDimensions(dimensions)

            const worker = async () => {
                const data = await decompressImageData(bitmapData.data)
                const bitmap = new Bitmap(data, {width: dimensions.width, height: dimensions.height})
                const palette = bitmap.getDynamicPalette(RR_BLUE, RR_ORANGE, RR_GREEN, RR_YELLOW)
                const roomLayers = [...bitmap.layers].filter(Bitmap.isRoom)
                setImageMap([...roomLayers, PixelType.OBSTACLE_WALL_V2].reduce((acc, room) => (
                    {...acc, [room]: bitmap.get(room, palette[room] ?? RR_GRAY)}
                ), {}))
                setRoomLabels(Object.keys(bitmap.rooms).reduce<Room[]>((acc, curr) => {
                    const mqttRoomId = parseInt(curr)
                    const dimensions = bitmap.rooms[mqttRoomId]
                    const roomFromMapping = roomList.find(roomFromMapping => roomFromMapping.mqtt_room_id === mqttRoomId)
                    const room: Room = {
                        name: roomFromMapping?.name ?? "no name",
                        room_id: roomFromMapping?.room_id ?? 0,
                        mqtt_room_id: mqttRoomId,
                        position: [
                            Math.floor(dimensions.start[0] + (dimensions.end[0] - dimensions.start[0]) / 2),
                            Math.floor(dimensions.start[1] + (dimensions.end[1] - dimensions.start[1]) / 2),
                        ]
                    }

                    return [...acc, room]
                }, []))
            }

            worker()
        }
    }, [bitmapData]);

    return <div className="relative">
        <svg width="100%"
             viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
             transform="scale(1,-1) rotate(180)">
            <defs>
                <filter x="-0.1" y="-0.25" width="1.2" height="1.5" id="solid">
                    <feFlood floodColor="rgba(159,159,159,0.7)"></feFlood>
                    <feComposite in="SourceGraphic"></feComposite>
                </filter>
                <filter id="saturate">
                    <feColorMatrix type="saturate" in="SourceGraphic" values="2"/>
                </filter>
            </defs>
            {
                imageMap && Object.keys(imageMap).map(pixelType => <image key={`type_${pixelType}`} x="0" y="0"
                                                                          width={dimensions.width}
                                                                          height={dimensions.height}
                                                                          href={imageMap[parseInt(pixelType)]}
                                                                          className={`room ${selectedRooms.length > 0 && !selectedRooms.includes(parseInt(pixelType)) ? "selected" : ""}`}
                                                                          style={{
                                                                              imageRendering: "pixelated"
                                                                          }}
                    />
                )
            }
            <rect x={chargerPosition[0] - 5}
                  y={chargerPosition[1] - 5}
                  width={10}
                  height={10}
                  rx={2}
                  fill="red"/>
            <circle cx={robotPosition[0]}
                    cy={robotPosition[1]}
                    r={5}
                    fill="blue"/>
            <polyline points={pathToPolylinePoints(path)}
                      fill="none" strokeDasharray="1 1"
                      stroke="magenta" strokeWidth="1"/>
            {virtualWalls.map(([start, end], index) =>
                <line key={index} x1={start[0]}
                      y1={start[1]}
                      x2={end[0]}
                      y2={end[1]}
                      strokeWidth={2} stroke="red" strokeDasharray="2 2"
                />
            )}
            {
                roomLabels.map(({mqtt_room_id, name, position: [x, y] = []}) => <text
                    key={mqtt_room_id}
                    style={{
                        transform: "scale(-1,1) translateY(3px)",
                        cursor: "pointer"
                    }}
                    onClick={() => {
                        if (selectedRooms.includes(mqtt_room_id)) {
                            setSelectedRooms(selectedRooms.filter(room => room != mqtt_room_id))
                        } else {
                            setSelectedRooms([...selectedRooms, mqtt_room_id])
                        }
                    }}
                    fill="rgba(0,0,0,0.9)"
                    filter="url(#solid)"
                    fontSize={6}
                    textAnchor="middle"
                    x={-1 * (x ?? 1)}
                    y={y}>{name}</text>)
            }

        </svg>
        {
            selectedRooms.length > 0 &&
            <button
                className="absolute right-40 top-20 px-4 py-2 bg-sky-800 text-white rounded-lg shadow-sm"
                onClick={cleanSegments}>Clean {selectedRooms.length} room{selectedRooms.length > 1 && 's'}</button>
        }
    </div>
}

const pathToPolylinePoints = (path: Path) => path.map(([x, y]) => `${x}, ${y}`).join(" ")

export default SvgMapClient