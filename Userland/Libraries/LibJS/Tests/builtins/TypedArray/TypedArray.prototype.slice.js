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
        expect(T.prototype.slice).toHaveLength(2);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        const first_slice = typedArray.slice(1, 2);
        expect(first_slice).toHaveLength(1);
        expect(first_slice[0]).toBe(2);

        const second_slice = typedArray.slice(3, 1);
        expect(second_slice).toHaveLength(0);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.slice).toHaveLength(2);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        const first_slice = typedArray.slice(1, 2);
        expect(first_slice).toHaveLength(1);
        expect(first_slice[0]).toBe(2n);

        const second_slice = typedArray.slice(3, 1);
        expect(second_slice).toHaveLength(0);
    });
});
