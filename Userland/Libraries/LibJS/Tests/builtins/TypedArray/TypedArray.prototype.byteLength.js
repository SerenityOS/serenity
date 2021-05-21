// Update when more typed arrays get added
const TYPED_ARRAYS = [
    { array: Uint8Array, expected: 3 },
    { array: Uint16Array, expected: 6 },
    { array: Uint32Array, expected: 12 },
    { array: Int8Array, expected: 3 },
    { array: Int16Array, expected: 6 },
    { array: Int32Array, expected: 12 },
    { array: Float32Array, expected: 12 },
    { array: Float64Array, expected: 24 },
];

test("basic functionality", () => {
    TYPED_ARRAYS.forEach(T => {
        const typedArray = new T.array([1, 2, 3]);
        expect(Object.hasOwn(typedArray, "byteOffset")).toBeFalse();
        expect(typedArray.byteLength).toBe(T.expected);
    });
});
