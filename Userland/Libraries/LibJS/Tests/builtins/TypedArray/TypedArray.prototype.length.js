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
        const typedArray = new T(42);
        expect(Object.hasOwnProperty(typedArray, "length")).toBeFalse();
        expect(typedArray.length).toBe(42);
    });
});
