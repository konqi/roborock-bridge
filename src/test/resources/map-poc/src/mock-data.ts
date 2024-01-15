import {BinaryData, ObjectPosition, Path, VirtualWalls} from "./types.ts";

export const mock_bitmap_data: BinaryData = {
    meta: {
        mineType: "application/octet-stream",
        dimensions: [393, 415]
    },
    mode: ["deflate", "base64"],
    data: "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"
}
export const mock_map_data: BinaryData = {
    meta: {
        mineType: "image/png",
        dimensions: []
    },
    mode: ["base64"],
    data: "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"
}

export const mock_room_mapping = [{"roomId": 11753835, "name": "Sunroom", "mqttRoomId": 24}, {
    "roomId": 11753771,
    "name": "Master bedroom",
    "mqttRoomId": 18
}, {"roomId": 11753769, "name": "Bathroom1", "mqttRoomId": 17}, {
    "roomId": 11753767,
    "name": "Kitchen",
    "mqttRoomId": 23
}, {"roomId": 11753766, "name": "Guest bedroom", "mqttRoomId": 21}, {
    "roomId": 11753761,
    "name": "Bathroom",
    "mqttRoomId": 20
}, {"roomId": 11753756, "name": "Corridor", "mqttRoomId": 19}, {
    "roomId": 11753751,
    "name": "Study",
    "mqttRoomId": 22
}, {"roomId": 11753748, "name": "Living room", "mqttRoomId": 16}]
export const mock_path: Path = [[187.24, 123.06], [187.2, 122.96], [186.94, 122.98], [185.54, 123.3], [185.36, 123.32], [185.26, 122.24], [185.24, 122.9], [186.16, 122.5], [186.62, 122.4], [188.28, 122.64], [190.26, 122.24], [192.16, 121.68], [193.64, 120.72], [194.42, 119.34], [195.86, 117.58], [196.48, 115.08], [195.68, 112.82], [195.48, 112.52], [195.58, 111.58], [195.62, 111.36], [195.7, 111.36], [196.98, 110.14], [196.16, 110.74], [197.02, 110.52], [197.54, 110.04], [199.82, 109.96], [202.48, 109.98], [202.68, 109.98], [202.68, 111.02], [203.28, 112.22], [203.34, 112.2], [203.32, 112.34], [202.5, 113.94], [202.6, 116.4], [204.1, 118.48], [206.28, 119.42], [208.94, 119.24], [210.72, 118.0], [211.94, 115.94], [211.88, 113.7], [211.04, 112.16], [210.7, 111.78], [211.1, 111.26], [211.66, 110.08], [212.04, 110.08], [214.9, 109.94], [215.66, 110.0], [214.66, 110.4], [213.52, 110.84], [213.48, 110.86], [213.48, 110.94], [213.48, 110.94], [213.68, 111.96], [213.74, 112.84], [213.7, 115.52], [215.2, 117.34], [214.82, 117.5], [213.68, 117.6], [214.54, 117.5], [215.58, 117.42], [217.64, 117.68], [218.36, 117.46], [218.58, 117.52], [219.44, 118.14], [220.22, 118.38], [222.12, 119.7], [224.4, 119.26], [224.82, 119.0], [226.14, 118.4], [226.84, 117.82], [226.8, 118.08], [228.56, 117.24], [229.48, 115.14], [228.96, 113.46], [229.02, 113.08], [229.24, 112.96], [230.86, 112.92], [232.28, 113.16], [234.2, 113.22], [234.26, 113.2], [234.02, 114.34], [234.58, 117.12], [236.36, 118.6], [238.7, 119.22], [241.08, 119.04], [242.68, 118.82], [244.9, 118.44], [246.26, 116.72], [247.4, 115.0], [247.0, 112.68], [245.4, 111.48], [244.68, 111.0], [243.68, 110.08], [243.54, 109.98], [243.7, 110.0], [244.52, 110.04], [245.94, 110.14], [246.28, 110.08], [248.04, 109.98], [249.54, 110.16], [250.4, 110.06], [250.68, 110.48], [251.6, 111.52], [251.64, 111.6], [250.64, 112.88], [250.18, 115.46], [251.06, 117.48], [252.8, 118.78], [255.58, 119.28], [257.74, 118.64], [259.24, 117.12], [259.8, 114.76], [259.64, 113.04], [258.98, 112.02], [258.46, 111.64], [258.42, 111.66], [259.32, 110.14], [259.36, 110.02], [261.64, 109.86], [265.12, 109.94], [266.04, 109.96], [266.18, 110.02], [266.68, 111.86], [267.1, 112.34], [267.22, 112.14], [266.3, 113.76], [266.24, 116.1], [267.5, 118.16], [269.3, 119.22], [271.3, 119.18], [271.82, 118.98], [271.84, 118.98], [272.34, 120.32], [272.36, 121.54], [272.28, 126.7], [272.28, 131.98], [272.22, 137.18], [272.18, 142.22], [272.16, 147.36], [272.04, 152.52], [272.04, 156.62], [272.12, 159.08], [272.18, 164.12], [272.26, 169.56], [272.42, 174.74], [272.5, 179.62], [272.54, 182.76], [272.38, 184.52], [272.82, 185.32], [272.76, 185.1], [272.2, 186.16], [271.62, 186.68], [271.3, 189.38], [271.74, 192.16], [271.66, 195.78], [271.72, 198.94], [271.68, 201.0], [271.66, 202.14], [271.66, 202.14], [269.28, 202.24], [267.72, 202.26], [264.26, 202.28], [261.0, 202.34], [257.42, 202.4], [255.18, 202.42], [253.26, 202.44], [251.58, 202.46], [249.56, 202.48], [245.8, 202.54], [241.64, 202.58], [237.66, 202.62], [233.66, 202.7], [231.42, 202.74], [229.38, 202.72], [227.58, 202.7], [225.76, 202.7], [223.12, 202.7], [220.5, 202.7], [218.66, 202.66], [216.66, 202.62], [214.8, 202.6], [212.98, 202.62], [212.12, 202.64], [211.9, 202.28], [211.42, 201.56], [211.34, 200.38], [211.42, 199.48], [212.02, 196.8], [210.9, 194.58], [209.08, 193.2], [206.28, 192.78], [204.22, 192.74], [201.44, 192.62], [197.88, 192.56], [195.4, 192.58], [192.16, 192.56], [188.14, 192.54], [184.42, 192.52], [182.42, 192.54], [179.0, 192.64], [175.3, 192.46], [175.14, 192.4], [174.86, 188.46], [175.04, 183.2], [175.36, 178.24], [175.54, 173.26], [175.64, 170.62], [175.66, 169.84], [175.68, 169.68], [178.84, 169.74], [182.04, 169.4], [183.92, 167.7], [184.82, 165.0], [184.6, 161.32], [184.66, 158.02], [184.74, 155.54], [184.76, 154.48], [184.8, 154.5], [186.94, 153.98], [188.06, 151.94], [187.48, 148.92], [186.18, 146.56], [184.92, 145.9], [184.82, 145.88], [184.68, 143.22], [184.84, 139.4], [184.82, 137.98], [184.8, 138.0], [186.44, 136.88], [187.54, 134.72], [187.48, 132.14], [186.4, 130.42], [184.96, 129.56], [184.94, 129.54], [184.66, 126.78], [184.74, 122.76], [184.72, 122.82], [185.04, 122.76], [185.92, 122.36], [187.16, 122.66], [188.08, 122.8], [187.92, 123.92], [187.48, 125.9], [187.26, 127.4], [187.26, 128.56], [187.52, 128.3], [189.48, 125.56], [190.18, 124.46], [190.24, 124.46], [190.28, 125.56], [190.26, 131.16], [190.1, 137.34], [190.06, 143.3], [190.04, 149.32], [190.04, 153.54], [190.04, 157.2], [190.04, 160.44], [190.0, 164.62], [189.92, 168.26], [189.96, 174.38], [189.98, 180.24], [190.0, 186.34], [190.1, 190.42], [189.86, 190.96], [188.64, 191.58], [187.8, 191.94], [187.74, 191.54], [187.72, 186.58], [187.78, 180.76], [188.02, 174.84], [188.08, 168.5], [188.1, 162.14], [187.98, 155.84], [187.86, 150.0], [187.9, 144.2], [187.92, 139.58], [188.56, 136.84], [190.42, 131.34], [191.56, 127.84], [191.78, 126.36], [191.82, 124.26], [191.78, 124.2], [191.84, 125.42], [191.88, 130.72], [191.82, 136.72], [191.8, 142.98], [191.78, 148.92], [191.78, 153.94], [191.8, 155.42], [191.8, 156.54], [191.64, 157.12], [190.64, 158.76], [190.5, 161.2], [191.68, 162.98], [192.9, 163.74], [194.84, 164.02], [196.78, 163.24], [197.64, 162.26], [197.98, 161.54], [198.02, 161.52], [198.54, 162.56], [199.82, 163.86], [201.08, 164.54], [203.5, 164.18], [205.38, 162.86], [205.98, 160.36], [205.18, 158.28], [203.62, 156.94], [201.24, 156.58], [199.46, 157.46], [198.96, 157.84], [198.14, 158.3], [198.04, 158.32], [197.2, 157.12], [194.98, 155.9], [192.76, 156.2], [191.1, 157.62], [190.9, 158.1], [190.9, 158.08], [192.08, 156.34], [193.4, 154.4], [193.8, 153.38], [193.8, 147.6], [193.62, 141.62], [193.54, 135.78], [193.5, 129.68], [193.46, 124.36], [193.9, 122.38], [195.4, 120.16], [195.84, 119.48], [195.84, 119.52], [195.88, 122.98], [195.88, 128.98], [195.8, 135.24], [195.78, 141.18], [195.72, 147.56], [195.7, 152.68], [196.06, 154.04], [197.32, 155.32], [197.3, 155.38], [197.34, 152.22], [197.24, 146.28], [197.18, 140.02], [197.12, 134.14], [197.12, 128.18], [197.1, 122.16], [197.1, 120.16], [197.06, 120.04], [197.06, 120.08], [195.16, 121.94], [192.76, 123.1], [191.02, 122.14], [192.04, 122.94], [192.86, 123.54], [192.84, 123.56], [191.58, 122.64], [191.5, 122.6], [192.44, 122.54], [196.42, 122.66], [197.76, 122.76], [198.3, 122.98], [198.4, 122.04], [198.32, 116.98], [198.24, 113.38], [198.18, 112.9], [198.2, 112.86], [198.32, 116.74], [198.46, 122.56], [198.44, 128.78], [198.36, 134.96], [198.36, 140.86], [198.3, 147.04], [198.32, 153.14], [198.34, 155.6], [198.76, 155.66], [200.58, 155.58], [200.58, 154.88], [200.42, 149.54], [200.36, 143.24], [200.32, 137.4], [200.28, 131.4], [200.28, 125.18], [200.22, 119.38], [200.18, 114.96], [200.2, 111.96], [200.16, 111.64], [200.36, 112.16], [201.8, 116.62], [202.82, 120.02], [203.12, 121.64], [203.12, 127.22], [203.1, 133.48], [203.06, 139.74], [203.02, 145.78], [203.1, 152.28], [203.12, 154.78], [204.6, 155.8], [205.54, 156.46], [205.56, 156.44], [205.54, 153.22], [205.36, 147.34], [205.34, 141.3], [205.34, 135.16], [205.34, 129.06], [205.32, 123.42], [205.38, 121.08], [206.38, 120.5], [208.36, 119.86], [208.34, 119.86], [208.36, 123.46], [208.34, 129.52], [208.22, 135.46], [208.2, 141.62], [208.18, 147.92], [208.18, 154.2], [208.18, 160.32], [208.04, 166.4], [208.06, 172.5], [207.98, 177.9], [208.0, 179.32], [208.04, 181.12], [208.02, 187.26], [208.14, 190.76], [207.94, 191.22], [206.54, 191.5], [205.24, 191.88], [205.14, 191.92], [205.1, 189.3], [205.08, 183.58], [205.1, 177.58], [205.16, 171.42], [205.14, 166.5], [205.14, 165.66], [203.98, 165.68], [203.42, 165.74], [203.4, 165.68], [203.38, 165.7], [203.42, 165.72], [203.36, 165.68], [203.32, 165.64], [201.48, 164.84], [199.04, 165.06], [197.8, 166.04], [197.6, 166.36], [197.54, 166.34], [197.12, 165.68], [195.46, 164.52], [193.58, 164.16], [191.62, 165.02], [190.28, 166.8], [189.96, 169.22], [191.0, 171.26], [193.2, 172.42], [195.62, 171.96], [196.74, 171.08], [196.94, 170.76], [196.96, 170.8], [196.94, 170.84], [196.94, 170.86], [196.92, 170.82], [198.18, 172.3], [200.44, 173.18], [202.78, 172.44], [204.24, 170.82], [204.78, 168.42], [203.98, 166.4], [202.7, 165.28], [201.04, 164.84], [201.02, 164.82], [201.02, 164.78], [202.78, 164.84], [205.76, 167.28], [207.26, 171.3], [207.86, 177.42], [207.64, 182.1], [207.28, 186.06], [208.02, 188.7], [209.0, 189.86], [210.2, 191.14], [210.3, 191.18], [210.38, 189.36], [210.14, 183.64], [209.88, 177.78], [209.78, 171.58], [209.74, 165.26], [209.76, 159.2], [209.8, 153.14], [209.76, 147.16], [209.74, 140.86], [209.8, 134.56], [209.8, 128.58], [209.8, 122.7], [209.86, 120.46], [211.5, 119.32], [212.38, 118.72], [212.38, 118.8], [212.46, 122.46], [212.42, 128.52], [212.36, 134.58], [212.26, 141.06], [212.2, 147.2], [212.18, 153.28], [212.2, 159.36], [212.18, 165.6], [212.12, 171.64], [212.12, 177.92], [212.02, 184.08], [212.16, 189.96], [212.24, 192.04], [213.54, 191.88], [214.1, 191.94], [214.22, 194.34], [214.26, 198.46], [214.3, 200.32], [214.32, 201.24], [214.3, 201.28], [214.34, 200.3], [214.36, 195.32], [214.3, 189.2], [214.14, 183.2], [214.04, 176.9], [214.1, 170.9], [214.16, 164.44], [214.22, 158.72], [214.24, 154.84], [214.24, 153.2], [214.24, 152.8], [214.22, 152.92], [216.86, 154.28], [219.38, 154.12], [221.14, 152.7], [222.08, 150.32], [221.96, 148.04], [220.78, 146.34], [218.76, 145.26], [216.04, 145.08], [214.06, 146.48], [213.1, 148.94], [213.24, 152.08], [215.16, 153.84], [216.06, 154.28], [216.1, 154.8], [216.18, 160.34], [216.16, 166.46], [216.08, 172.54], [216.08, 178.78], [216.08, 184.92], [216.1, 191.1], [216.12, 197.04], [216.18, 200.02], [216.22, 201.5], [216.36, 201.68], [218.2, 201.56], [218.78, 201.5], [218.76, 200.36], [218.74, 194.76], [218.6, 188.72], [218.4, 182.42], [218.28, 176.28], [218.26, 170.2], [218.36, 163.98], [218.52, 157.94], [218.56, 155.26], [219.68, 155.18], [221.3, 155.12], [221.6, 155.16], [221.66, 159.26], [221.62, 165.2], [221.52, 171.3], [221.4, 177.8], [221.38, 178.74], [220.62, 178.8], [218.62, 179.96], [218.0, 182.22], [218.94, 183.94], [219.54, 184.62], [221.6, 186.38], [223.92, 186.68], [226.18, 185.4], [227.14, 183.26], [226.86, 181.24], [225.16, 179.24], [222.58, 178.7], [220.26, 180.02], [220.18, 180.14], [220.36, 179.96], [222.06, 178.08], [222.92, 177.0], [222.84, 173.86], [222.78, 168.12], [222.74, 161.86], [222.78, 155.96], [222.88, 152.88], [223.4, 150.48], [223.56, 148.28], [223.56, 142.44], [223.52, 136.38], [223.48, 130.16], [223.42, 124.6], [223.36, 121.88], [221.92, 120.78], [220.86, 119.9], [220.36, 119.46], [220.34, 120.12], [220.46, 123.98], [220.46, 126.44], [220.46, 127.92], [220.46, 129.1], [220.42, 129.12], [219.62, 128.68], [217.1, 128.08], [214.5, 128.7], [213.16, 130.68], [212.86, 133.52], [213.56, 135.56], [214.84, 136.8], [217.74, 137.3], [219.92, 136.54], [221.26, 135.18], [221.94, 132.56], [221.44, 130.66], [220.08, 129.2], [217.66, 128.4], [217.62, 128.4], [217.48, 128.18], [217.56, 127.94], [217.58, 123.44], [217.5, 120.12], [216.1, 118.96], [215.08, 118.28], [214.98, 118.22], [214.92, 120.28], [215.0, 124.58], [215.04, 126.32], [215.38, 126.12], [219.76, 123.84], [223.62, 121.84], [224.68, 121.22], [224.68, 121.22], [224.78, 123.72], [224.68, 129.96], [224.58, 136.26], [224.56, 142.26], [224.5, 148.78], [224.5, 154.94], [224.54, 161.16], [224.52, 167.5], [224.52, 173.66], [224.54, 177.26], [224.5, 177.78], [224.48, 177.84], [224.46, 175.86], [224.52, 170.48], [224.66, 164.1], [224.8, 157.64], [224.92, 152.82], [224.96, 148.84], [223.96, 145.96], [222.88, 144.78], [221.68, 143.6], [221.68, 141.14], [221.7, 138.2], [221.58, 138.06], [219.72, 138.1], [218.74, 138.1], [218.68, 139.4], [218.74, 142.46], [218.54, 142.94], [216.72, 143.6], [216.22, 143.84], [216.14, 142.38], [216.18, 139.02], [216.18, 138.64], [214.24, 138.74], [214.28, 139.08], [214.28, 142.26], [214.3, 142.82], [214.82, 142.46], [218.18, 140.44], [221.16, 137.76], [223.24, 134.56], [223.92, 130.74], [224.02, 126.68], [224.76, 123.44], [225.86, 122.0], [227.12, 120.82], [227.16, 120.7], [227.24, 122.8], [227.08, 128.96], [226.88, 134.82], [226.8, 141.14], [226.78, 147.72], [226.82, 154.14], [226.78, 159.94], [226.82, 165.58], [226.8, 172.42], [226.78, 177.28], [226.8, 178.34], [228.68, 178.26], [229.06, 178.14], [229.16, 180.62], [229.06, 186.92], [229.0, 193.28], [228.88, 198.26], [228.92, 200.9], [228.98, 201.58], [229.08, 201.36], [229.08, 199.72], [229.06, 194.28], [228.94, 188.32], [228.88, 182.06], [228.76, 175.66], [228.78, 169.3], [228.92, 162.94], [229.02, 156.98], [229.0, 151.14], [229.0, 148.78], [228.98, 147.54], [228.92, 147.92], [229.14, 148.2], [231.3, 149.22], [233.68, 148.82], [235.54, 147.46], [235.9, 146.56], [235.9, 146.58], [236.4, 147.3], [238.08, 148.74], [240.64, 149.12], [242.54, 148.04], [243.46, 146.32], [243.52, 143.9], [242.36, 142.08], [240.72, 141.14], [238.72, 141.16], [236.86, 142.24], [236.38, 142.66], [235.9, 142.9], [235.8, 143.0], [234.78, 141.82], [233.4, 141.04], [231.42, 140.78], [229.28, 141.88], [228.12, 143.9], [228.28, 146.62], [230.22, 148.48], [231.04, 149.06], [231.06, 149.8], [231.12, 155.34], [231.12, 161.34], [231.12, 167.72], [231.12, 173.84], [231.1, 180.34], [231.12, 186.5], [231.18, 192.3], [231.2, 197.78], [231.12, 200.6], [231.2, 201.64], [232.64, 201.54], [233.7, 201.44], [233.78, 199.72], [233.74, 194.06], [233.58, 188.1], [233.34, 181.88], [233.4, 175.84], [233.44, 169.52], [233.56, 162.72], [233.6, 156.78], [233.62, 152.06], [234.32, 150.94], [235.94, 149.64], [236.14, 149.46], [236.22, 151.14], [236.14, 157.08], [236.06, 163.52], [236.06, 169.34], [235.98, 175.72], [235.92, 181.9], [236.0, 188.3], [236.04, 194.24], [236.04, 199.06], [235.98, 200.58], [237.54, 201.08], [238.56, 201.28], [238.56, 201.02], [238.54, 196.38], [238.38, 190.72], [238.2, 184.52], [238.12, 178.28], [238.14, 172.24], [238.22, 165.68], [238.34, 159.38], [238.38, 153.3], [238.4, 151.06], [239.76, 150.4], [241.04, 149.98], [241.5, 149.84], [241.48, 151.7], [241.42, 157.58], [241.26, 164.08], [241.24, 170.42], [241.24, 176.44], [241.22, 182.74], [241.22, 189.08], [241.22, 195.0], [241.16, 199.52], [241.18, 200.82], [242.94, 201.3], [243.62, 201.4], [243.64, 200.62], [243.56, 195.3], [243.46, 189.6], [243.5, 183.26], [243.44, 176.88], [243.52, 170.8], [243.54, 164.56], [243.52, 158.24], [243.5, 152.24], [243.52, 150.06], [244.98, 150.02], [245.76, 149.98], [245.8, 148.2], [245.74, 142.26], [245.7, 136.24], [245.58, 130.32], [245.52, 124.14], [245.46, 119.94], [245.46, 119.46], [245.46, 119.52], [245.62, 123.94], [245.56, 130.04], [245.52, 136.14], [245.54, 141.2], [245.54, 145.5], [245.5, 151.68], [245.54, 157.98], [245.38, 164.2], [245.52, 170.56], [245.6, 176.9], [245.52, 183.02], [245.5, 189.22], [245.46, 195.42], [245.5, 199.46], [245.52, 200.88], [246.44, 201.04], [247.72, 201.24], [248.62, 201.5]]
export const mock_robot_position: ObjectPosition = {"x": 186.28, "y": 109.68, "a": 91}
export const mock_charger_position: ObjectPosition = {"x": 186.28, "y": 109.68, "a": 91}
export const mock_virtual_walls: VirtualWalls = [[{"x": 277.28, "y": 217.3}, {"x": 278.36, "y": 89.3}], [{
    "x": 58.4,
    "y": 283.72
}, {"x": 135.84, "y": 283.78}], [{"x": 176.42, "y": 264.42}, {"x": 223.82, "y": 264.42}], [{
    "x": 119.72,
    "y": 244.88
}, {"x": 138.68, "y": 244.88}], [{"x": 119.68, "y": 245.22}, {"x": 119.48, "y": 263.3}]]