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
        expect(T.prototype.includes).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        expect(typedArray.includes(2)).toBe(true);
        expect(typedArray.includes(-1)).toBe(false);
        expect(typedArray.includes(Infinity)).toBe(false);
        expect(typedArray.includes(2, 2)).toBe(false);
        expect(typedArray.includes(2, -2)).toBe(true);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.includes).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        expect(typedArray.includes(2n)).toBe(true);
        expect(typedArray.includes(-1n)).toBe(false);
        expect(typedArray.includes(2n, 2)).toBe(false);
        expect(typedArray.includes(2n, -2)).toBe(true);
    });
});
