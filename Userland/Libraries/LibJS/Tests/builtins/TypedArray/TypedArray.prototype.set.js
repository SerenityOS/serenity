const TYPED_ARRAYS = [
    { array: Uint8Array, maxUnsignedInteger: 2 ** 8 - 1 },
    { array: Uint8ClampedArray, maxUnsignedInteger: 2 ** 8 - 1 },
    { array: Uint16Array, maxUnsignedInteger: 2 ** 16 - 1 },
    { array: Uint32Array, maxUnsignedInteger: 2 ** 32 - 1 },
    { array: Int8Array, maxUnsignedInteger: 2 ** 7 - 1 },
    { array: Int16Array, maxUnsignedInteger: 2 ** 15 - 1 },
    { array: Int32Array, maxUnsignedInteger: 2 ** 31 - 1 },
    { array: Float32Array, maxUnsignedInteger: 2 ** 24 - 1 },
    { array: Float64Array, maxUnsignedInteger: Number.MAX_SAFE_INTEGER },
];

const BIGINT_TYPED_ARRAYS = [
    { array: BigUint64Array, maxUnsignedInteger: 2n ** 64n - 1n },
    { array: BigInt64Array, maxUnsignedInteger: 2n ** 63n - 1n },
];

// FIXME: Write out a full test suite for this function. This currently only performs a single regression test.
describe("normal behavior", () => {
    // Previously, we didn't apply source's byte offset on the code path for setting a typed array
    // from another typed array of the same type. This means the result array would previously contain
    // [maxUnsignedInteger - 3(n), maxUnsignedInteger - 2(n)] instead of [maxUnsignedInteger - 1(n), maxUnsignedInteger]
    test("two typed arrays of the same type code path applies source's byte offset", () => {
        TYPED_ARRAYS.forEach(({ array, maxUnsignedInteger }) => {
            const firstTypedArray = new array([
                maxUnsignedInteger - 3,
                maxUnsignedInteger - 2,
                maxUnsignedInteger - 1,
                maxUnsignedInteger,
            ]);
            const secondTypedArray = new array(2);
            secondTypedArray.set(firstTypedArray.subarray(2, 4), 0);
            expect(secondTypedArray[0]).toBe(maxUnsignedInteger - 1);
            expect(secondTypedArray[1]).toBe(maxUnsignedInteger);
        });

        BIGINT_TYPED_ARRAYS.forEach(({ array, maxUnsignedInteger }) => {
            const firstTypedArray = new array([
                maxUnsignedInteger - 3n,
                maxUnsignedInteger - 2n,
                maxUnsignedInteger - 1n,
                maxUnsignedInteger,
            ]);
            const secondTypedArray = new array(2);
            secondTypedArray.set(firstTypedArray.subarray(2, 4), 0);
            expect(secondTypedArray[0]).toBe(maxUnsignedInteger - 1n);
            expect(secondTypedArray[1]).toBe(maxUnsignedInteger);
        });
    });
});
