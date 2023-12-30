import {Path, Position, VirtualWalls} from "./types.ts";
import {useEffect, useState} from "react";

interface SvgMapProps {
    imageUrl: string
    robotPosition: Position
    chargerPosition: Position
    path: Path
    virtualWalls: VirtualWalls
}

function SvgMap({imageUrl, chargerPosition, robotPosition, path, virtualWalls}: SvgMapProps) {
    const [dimensions, setDimensions] = useState({width: 0, height: 0})

    useEffect(() => {
        (async () => {
            if (imageUrl) {
                const image = await measureImageDataUrl(imageUrl)
                setDimensions({width: image.width, height: image.height})
            }
        })()

    }, [imageUrl]);

    return <svg width="100%"
                viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
                transform="scale(1,-1) rotate(180)">
        <image x="0"
               y="0"
               width={dimensions.width}
               height={dimensions.height}
               href={imageUrl}/>
        <rect x={robotPosition.x - 5}
              y={robotPosition.y - 5}
              width={10}
              height={10}
              rx={2}
              fill="red"/>
        <circle cx={chargerPosition.x}
                cy={chargerPosition.y}
                r={5}
                fill="blue"/>
        <polyline points={pathToPolylinePoints(path)}
                  fill="none" strokeDasharray="1 1"
                  stroke="magenta" strokeWidth="1"/>
        {virtualWalls.map(([start, end]) =>
            <line x1={start.x}
                  y1={start.y}
                  x2={end.x}
                  y2={end.y}
                  strokeWidth={2} stroke="red" strokeDasharray="2 2"/>
        )}
    </svg>
}

const pathToPolylinePoints = (path: Path) => path.map(({x, y}) => `${x}, ${y}`).join(" ")

const measureImageDataUrl = (imageData: string): Promise<HTMLImageElement> => new Promise(resolve => {
    const image = new Image()
    image.addEventListener("load", () => {
        resolve(image)
    })

    image.src = imageData
})

export default SvgMap