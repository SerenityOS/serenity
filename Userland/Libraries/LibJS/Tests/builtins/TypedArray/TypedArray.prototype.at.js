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
        expect(T.prototype.at).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        expect(typedArray.at(0)).toBe(1);
        expect(typedArray.at(1)).toBe(2);
        expect(typedArray.at(2)).toBe(3);
        expect(typedArray.at(3)).toBeUndefined();
        expect(typedArray.at(Infinity)).toBeUndefined();
        expect(typedArray.at(-1)).toBe(3);
        expect(typedArray.at(-2)).toBe(2);
        expect(typedArray.at(-3)).toBe(1);
        expect(typedArray.at(-4)).toBeUndefined();
        expect(typedArray.at(-Infinity)).toBeUndefined();
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.at).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        expect(typedArray.at(0)).toBe(1n);
        expect(typedArray.at(1)).toBe(2n);
        expect(typedArray.at(2)).toBe(3n);
        expect(typedArray.at(3)).toBeUndefined();
        expect(typedArray.at(Infinity)).toBeUndefined();
        expect(typedArray.at(-1)).toBe(3n);
        expect(typedArray.at(-2)).toBe(2n);
        expect(typedArray.at(-3)).toBe(1n);
        expect(typedArray.at(-4)).toBeUndefined();
        expect(typedArray.at(-Infinity)).toBeUndefined();
    });
});
