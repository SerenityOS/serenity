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

test("length is 2", () => {
    TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.copyWithin).toHaveLength(2);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.copyWithin).toHaveLength(2);
    });
});

describe("normal behavior", () => {
    test("Noop", () => {
        TYPED_ARRAYS.forEach(T => {
            const array = new T([1, 2]);
            expect(array.copyWithin(0, 0)).toEqual(array);
            expect(array).toEqual([1, 2]);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const array = new T([1n, 2n]);
            expect(array.copyWithin(0, 0)).toEqual(array);
            expect(array).toEqual([1n, 2n]);
        });
    });

    test("basic behavior", () => {
        TYPED_ARRAYS.forEach(T => {
            const array = new T([1, 2, 3]);
            expect(array.copyWithin(1, 2)).toEqual(array);
            expect(array).toEqual([1, 3, 3]);

            expect(array.copyWithin(2, 0)).toEqual(array);
            expect(array).toEqual([1, 3, 1]);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const array = new T([1n, 2n, 3n]);
            expect(array.copyWithin(1, 2)).toEqual(array);
            expect(array).toEqual([1n, 3n, 3n]);

            expect(array.copyWithin(2, 0)).toEqual(array);
            expect(array).toEqual([1n, 3n, 1n]);
        });
    });

    test("start > target", () => {
        TYPED_ARRAYS.forEach(T => {
            const array = new T([1, 2, 3]);
            expect(array.copyWithin(0, 1)).toEqual(array);
            expect(array).toEqual([2, 3, 3]);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const array = new T([1n, 2n, 3n]);
            expect(array.copyWithin(0, 1)).toEqual(array);
            expect(array).toEqual([2n, 3n, 3n]);
        });
    });

    test("overwriting behavior", () => {
        TYPED_ARRAYS.forEach(T => {
            const array = new T([1, 2, 3]);
            expect(array.copyWithin(1, 0)).toEqual(array);
            expect(array).toEqual([1, 1, 2]);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const array = new T([1n, 2n, 3n]);
            expect(array.copyWithin(1, 0)).toEqual(array);
            expect(array).toEqual([1n, 1n, 2n]);
        });
    });

    test("specify end", () => {
        TYPED_ARRAYS.forEach(T => {
            const array = new T([1, 2, 3]);
            expect(array.copyWithin(2, 0, 1)).toEqual(array);
            expect(array).toEqual([1, 2, 1]);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const array = new T([1n, 2n, 3n]);
            expect(array.copyWithin(2, 0, 1)).toEqual(array);
            expect(array).toEqual([1n, 2n, 1n]);
        });
    });
});
