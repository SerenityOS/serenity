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
            expect(T.prototype.toSpliced).toHaveLength(2);
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            expect(T.prototype.toSpliced).toHaveLength(2);
        });
    });

    test("no start or delete count argument", () => {
        TYPED_ARRAYS.forEach(T => {
            const a = new T([1, 2, 3, 4, 5]);
            const b = a.toSpliced();
            expect(a).not.toBe(b);
            expect(a).toEqual([1, 2, 3, 4, 5]);
            expect(b).toEqual([1, 2, 3, 4, 5]);
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            const a = new T([1n, 2n, 3n, 4n, 5n]);
            const b = a.toSpliced();
            expect(a).not.toBe(b);
            expect(a).toEqual([1n, 2n, 3n, 4n, 5n]);
            expect(b).toEqual([1n, 2n, 3n, 4n, 5n]);
        });
    });

    test("only start argument", () => {
        TYPED_ARRAYS.forEach(T => {
            const a = new T([1, 2, 3, 4, 5]);
            const values = [
                [0, [0, 0, 0, 0, 0]],
                [1, [1, 0, 0, 0, 0]],
                [4, [1, 2, 3, 4, 0]],
                [-1, [1, 2, 3, 4, 0]],
                [999, [1, 2, 3, 4, 5]],
                [Infinity, [1, 2, 3, 4, 5]],
            ];
            for (const [start, expected] of values) {
                const b = a.toSpliced(start);
                expect(a).not.toBe(b);
                expect(a).toEqual([1, 2, 3, 4, 5]);
                expect(b).toEqual(expected);
            }
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            const a = new T([1n, 2n, 3n, 4n, 5n]);
            const values = [
                [0, [0n, 0n, 0n, 0n, 0n]],
                [1, [1n, 0n, 0n, 0n, 0n]],
                [4, [1n, 2n, 3n, 4n, 0n]],
                [-1, [1n, 2n, 3n, 4n, 0n]],
                [999, [1n, 2n, 3n, 4n, 5n]],
                [Infinity, [1n, 2n, 3n, 4n, 5n]],
            ];
            for (const [start, expected] of values) {
                const b = a.toSpliced(start);
                expect(a).not.toBe(b);
                expect(a).toEqual([1n, 2n, 3n, 4n, 5n]);
                expect(b).toEqual(expected);
            }
        });
    });

    test("start and delete count argument", () => {
        TYPED_ARRAYS.forEach(T => {
            const a = new T([1, 2, 3, 4, 5]);
            const values = [
                [0, 5, [0, 0, 0, 0, 0]],
                [1, 3, [1, 5, 0, 0, 0]],
                [4, 1, [1, 2, 3, 4, 0]],
                [-1, 1, [1, 2, 3, 4, 0]],
                [999, 10, [1, 2, 3, 4, 5]],
                [Infinity, Infinity, [1, 2, 3, 4, 5]],
            ];
            for (const [start, deleteCount, expected] of values) {
                const b = a.toSpliced(start, deleteCount);
                expect(a).not.toBe(b);
                expect(a).toEqual([1, 2, 3, 4, 5]);
                expect(b).toEqual(expected);
            }
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            const a = new T([1n, 2n, 3n, 4n, 5n]);
            const values = [
                [0, 5, [0n, 0n, 0n, 0n, 0n]],
                [1, 3, [1n, 5n, 0n, 0n, 0n]],
                [4, 1, [1n, 2n, 3n, 4n, 0n]],
                [-1, 1, [1n, 2n, 3n, 4n, 0n]],
                [999, 10, [1n, 2n, 3n, 4n, 5n]],
                [Infinity, Infinity, [1n, 2n, 3n, 4n, 5n]],
            ];
            for (const [start, deleteCount, expected] of values) {
                const b = a.toSpliced(start, deleteCount);
                expect(a).not.toBe(b);
                expect(a).toEqual([1n, 2n, 3n, 4n, 5n]);
                expect(b).toEqual(expected);
            }
        });
    });
});

describe("errors", () => {
    test("null or undefined this value", () => {
        TYPED_ARRAYS.forEach(T => {
            expect(() => {
                T.prototype.toSpliced.call();
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSpliced.call(undefined);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSpliced.call(null);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            expect(() => {
                T.prototype.toSpliced.call();
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSpliced.call(undefined);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSpliced.call(null);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
        });
    });

    test("invalid typed array length", () => {
        TYPED_ARRAYS.forEach(T => {
            expect(() => {
                new T({ length: 2 ** 32 - 1 });
            }).toThrowWithMessage(RangeError, "Invalid typed array length");
        });
        BIGINT_TYPED_ARRAYS.forEach(T => {
            expect(() => {
                new T({ length: 2 ** 32 - 1 });
            }).toThrowWithMessage(RangeError, "Invalid typed array length");
        });
    });
});
