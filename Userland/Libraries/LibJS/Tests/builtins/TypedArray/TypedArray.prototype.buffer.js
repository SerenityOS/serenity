// Update when more typed arrays get added
const TYPED_ARRAYS = [
    Uint8Array,
    Uint16Array,
    Uint32Array,
    Int8Array,
    Int16Array,
    Int32Array,
    Float32Array,
    Float64Array,
];

test("basic functionality", () => {
    TYPED_ARRAYS.forEach(T => {
        const typedArray = new T([1, 2, 3]);
        expect(Object.hasOwn(typedArray, "byteOffset")).toBeFalse();

        const buffer = typedArray.buffer;
        expect(buffer).toBeInstanceOf(ArrayBuffer);
        expect(buffer.byteLength).toBe(typedArray.byteLength);
        expect(buffer.byteLength).toBe(typedArray.length * typedArray.BYTES_PER_ELEMENT);

        const arrayFromBuffer = new T(buffer);
        expect(arrayFromBuffer.length).toBe(typedArray.length);
        expect(arrayFromBuffer.byteLength).toBe(typedArray.byteLength);
        expect(arrayFromBuffer.byteOffset).toBe(typedArray.byteOffset);
        expect(arrayFromBuffer[0]).toBe(typedArray[0]);
        expect(arrayFromBuffer[1]).toBe(typedArray[1]);
        expect(arrayFromBuffer[2]).toBe(typedArray[2]);
    });
});
