const TYPED_ARRAYS = [
    Uint8Array,
    Uint8ClampedArray,
    Uint16Array,
    Uint32Array,
    BigUint64Array,
    Int8Array,
    Int16Array,
    Int32Array,
    BigInt64Array,
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
