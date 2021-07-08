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
        expect(T.prototype.reduceRight).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1;
        typedArray[1] = 2;
        typedArray[2] = 3;

        expect(typedArray.reduceRight((accumulator, value) => accumulator + value)).toBe(6);
        expect(typedArray.reduceRight((accumulator, value) => accumulator + value, -5)).toBe(1);

        const order = [];
        typedArray.reduceRight((accumulator, value) => order.push(value), 0);
        expect(order).toEqual([3, 2, 1]);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.reduceRight).toHaveLength(1);

        const typedArray = new T(3);
        typedArray[0] = 1n;
        typedArray[1] = 2n;
        typedArray[2] = 3n;

        expect(typedArray.reduceRight((accumulator, value) => accumulator + value)).toBe(6n);
        expect(typedArray.reduceRight((accumulator, value) => accumulator + value, -5n)).toBe(1n);

        const order = [];
        typedArray.reduceRight((accumulator, value) => order.push(value), 0);
        expect(order).toEqual([3n, 2n, 1n]);
    });
});
