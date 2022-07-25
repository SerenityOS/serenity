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
        expect(T.prototype.toSorted).toHaveLength(1);

        const typedArray = new T([3, 1, 2]);
        let sortedtypedArray = typedArray.toSorted();
        expect(sortedtypedArray).not.toBe(typedArray);
        expect(sortedtypedArray[0]).toBe(1);
        expect(sortedtypedArray[1]).toBe(2);
        expect(sortedtypedArray[2]).toBe(3);

        sortedtypedArray = typedArray.toSorted(() => 0);
        expect(sortedtypedArray).not.toBe(typedArray);
        expect(sortedtypedArray[0]).toBe(3);
        expect(sortedtypedArray[1]).toBe(1);
        expect(sortedtypedArray[2]).toBe(2);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.toSorted).toHaveLength(1);

        const typedArray = new T([3n, 1n, 2n]);

        let sortedtypedArray = typedArray.toSorted();
        expect(sortedtypedArray).not.toBe(typedArray);
        expect(sortedtypedArray[0]).toBe(1n);
        expect(sortedtypedArray[1]).toBe(2n);
        expect(sortedtypedArray[2]).toBe(3n);

        sortedtypedArray = typedArray.toSorted(() => 0);
        expect(sortedtypedArray).not.toBe(typedArray);
        expect(sortedtypedArray[0]).toBe(3n);
        expect(sortedtypedArray[1]).toBe(1n);
        expect(sortedtypedArray[2]).toBe(2n);
    });
});

describe("errors", () => {
    test("null or undefined this value", () => {
        TYPED_ARRAYS.forEach(T => {
            expect(() => {
                T.prototype.toSorted.call();
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSorted.call(undefined);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

            expect(() => {
                T.prototype.toSorted.call(null);
            }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {});
    });

    test("invalid compare function", () => {
        TYPED_ARRAYS.forEach(T => {
            expect(() => {
                new T([]).toSorted("foo");
            }).toThrowWithMessage(TypeError, "foo is not a function");
        });

        BIGINT_TYPED_ARRAYS.forEach(T => {
            expect(() => {
                new T([]).toSorted("foo");
            }).toThrowWithMessage(TypeError, "foo is not a function");
        });
    });
});
