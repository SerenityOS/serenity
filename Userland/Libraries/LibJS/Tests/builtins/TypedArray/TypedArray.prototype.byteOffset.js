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
        expect(typedArray.byteOffset).toBe(0);
        expect(typedArray.length).toBe(3);

        const buffer = typedArray.buffer;

        const arrayFromOffset = new T(buffer, T.BYTES_PER_ELEMENT);
        expect(arrayFromOffset.byteOffset).toBe(T.BYTES_PER_ELEMENT);
        expect(arrayFromOffset.length).toBe(2);
        expect(arrayFromOffset[0]).toBe(2);
        expect(arrayFromOffset[1]).toBe(3);
    });
});
