import {BinaryData, ObjectPosition, Path, VirtualWalls} from "./types.ts";
import {useEffect, useState} from "react";

const pathToPolylinePoints = (path: Path) => path.map(([x, y]) => `${x}, ${y}`).join(" ")

const measureImageDataUrl = (imageData: string): Promise<HTMLImageElement> => new Promise(resolve => {
    const image = new Image()
    image.addEventListener("load", () => {
        resolve(image)
    })

    image.src = imageData
})

interface SvgMapProps {
    imageUrl: BinaryData
    robotPosition: ObjectPosition
    chargerPosition: ObjectPosition
    path: Path
    virtualWalls: VirtualWalls,
}

function SvgMap({
                    chargerPosition,
                    robotPosition,
                    path,
                    virtualWalls,
                    imageUrl
                }: SvgMapProps) {
    const [dimensions, setDimensions] = useState({width: 0, height: 0})

    useEffect(() => {
            const worker = async () => {
                if (imageUrl) {
                    const image = await measureImageDataUrl(`data:image/png;base64,${imageUrl.data}`)
                    setDimensions({width: image.width, height: image.height})
                }
            }

            worker()


    }, [imageUrl]);

    return <svg width="100%"
                viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
                transform="scale(1,-1) rotate(180)">
            <image x="0"
                   y="0"
                   width={dimensions.width}
                   height={dimensions.height}
                   href={`data:image/png;base64,${imageUrl.data}`}/>
        <rect x={robotPosition[0] - 5}
              y={robotPosition[1] - 5}
              width={10}
              height={10}
              rx={2}
              fill="red"/>
        <circle cx={chargerPosition[0]}
                cy={chargerPosition[1]}
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
    </svg>
}

export default SvgMap