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
        expect(T.prototype.subarray).toHaveLength(2);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        const subarray = typedArray.subarray(1, 2);
        expect(subarray).toHaveLength(1);
        expect(subarray[0]).toBe(2);
        subarray[0] = 4;
        expect(typedArray[1]).toBe(4);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.subarray).toHaveLength(2);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        const subarray = typedArray.subarray(1, 2);
        expect(subarray).toHaveLength(1);
        expect(subarray[0]).toBe(2n);
        subarray[0] = 4n;
        expect(typedArray[1]).toBe(4n);
    });
});
