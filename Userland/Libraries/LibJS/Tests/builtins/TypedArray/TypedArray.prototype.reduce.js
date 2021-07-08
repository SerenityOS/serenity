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
        expect(T.prototype.reduce).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        expect(typedArray.reduce((accumulator, value) => accumulator + value)).toBe(6);
        expect(typedArray.reduce((accumulator, value) => accumulator + value, -5)).toBe(1);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.reduce).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        expect(typedArray.reduce((accumulator, value) => accumulator + value)).toBe(6n);
        expect(typedArray.reduce((accumulator, value) => accumulator + value, -5n)).toBe(1n);
    });
});
