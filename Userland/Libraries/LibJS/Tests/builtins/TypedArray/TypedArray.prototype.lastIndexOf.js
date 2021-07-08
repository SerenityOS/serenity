const TYPED_ARRAYS = [
    Uint8Array,
    Uint8ClampedArray,
    Uint16Array,
    Uint32Array,
    Int8Array,
    Int16Array,
    Int32Array,
    Float32Array,
    Float64Array,
];

const BIGINT_TYPED_ARRAYS = [BigUint64Array, BigInt64Array];

test("basic functionality", () => {
    TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.lastIndexOf).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 2;

        expect(typedArray.lastIndexOf(2)).toBe(2);
        expect(typedArray.lastIndexOf(-1)).toBe(-1);
        expect(typedArray.lastIndexOf(Infinity)).toBe(-1);
        expect(typedArray.lastIndexOf(2, 2)).toBe(2);
        expect(typedArray.lastIndexOf(2, -2)).toBe(1);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.lastIndexOf).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 2n;

        expect(typedArray.lastIndexOf(2n)).toBe(2);
        expect(typedArray.lastIndexOf(-1n)).toBe(-1);
        expect(typedArray.lastIndexOf(2n, 2)).toBe(2);
        expect(typedArray.lastIndexOf(2n, -2)).toBe(1);
    });
});
