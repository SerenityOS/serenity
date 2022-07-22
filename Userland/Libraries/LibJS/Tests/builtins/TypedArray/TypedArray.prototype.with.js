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

describe("normal behavior", () => {
    test("length is 2", () => {
        TYPED_ARRAYS.forEach(T => {
            expect(T.prototype.with).toHaveLength(2);
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            expect(T.prototype.with).toHaveLength(2);
        });
    });

    test("basic functionality", () => {
        TYPED_ARRAYS.forEach(T => {
            const a = new T([1, 2, 3, 4, 5]);
            const values = [
                [0, 10, [10, 2, 3, 4, 5]],
                [-5, 10, [10, 2, 3, 4, 5]],
                [4, 10, [1, 2, 3, 4, 10]],
                [-1, 10, [1, 2, 3, 4, 10]],
            ];
            for (const [index, value, expected] of values) {
                const b = a.with(index, value);
                expect(a).not.toBe(b);
                expect(a).toEqual([1, 2, 3, 4, 5]);
                expect(b).toEqual(expected);
            }
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            const a = new T([1n, 2n, 3n, 4n, 5n]);
            const values = [
                [0, 10n, [10n, 2n, 3n, 4n, 5n]],
                [-5, 10n, [10n, 2n, 3n, 4n, 5n]],
                [4, 10n, [1n, 2n, 3n, 4n, 10n]],
                [-1, 10n, [1n, 2n, 3n, 4n, 10n]],
            ];
            for (const [index, value, expected] of values) {
                const b = a.with(index, value);
                expect(a).not.toBe(b);
                expect(a).toEqual([1n, 2n, 3n, 4n, 5n]);
                expect(b).toEqual(expected);
            }
        });
    });
});
