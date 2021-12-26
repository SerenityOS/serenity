test("invariants", () => {
    expect(Atomics.or).toHaveLength(3);
});

test("error cases", () => {
    expect(() => {
        Atomics.or("not an array", 0, 1);
    }).toThrow(TypeError);

    expect(() => {
        const bad_array_type = new Float32Array(4);
        Atomics.or(bad_array_type, 0, 1);
    }).toThrow(TypeError);

    expect(() => {
        const bad_array_type = new Uint8ClampedArray(4);
        Atomics.or(bad_array_type, 0, 1);
    }).toThrow(TypeError);

    expect(() => {
        const array = new Int32Array(4);
        Atomics.or(array, 100, 1);
    }).toThrow(RangeError);
});

test("basic functionality (non-BigInt)", () => {
    [Int8Array, Int16Array, Int32Array, Uint8Array, Uint16Array, Uint32Array].forEach(ArrayType => {
        const array = new ArrayType(2);
        array[0] = 0b0000;
        array[1] = 0b0101;

        expect(Atomics.or(array, 0, 0b0000)).toBe(0b0000);
        expect(array).toEqual([0b0000, 0b0101]);

        expect(Atomics.or(array, 0, 0b1111)).toBe(0b0000);
        expect(array).toEqual([0b1111, 0b0101]);

        expect(Atomics.or(array, 1, 0b0101)).toBe(0b0101);
        expect(array).toEqual([0b1111, 0b0101]);

        expect(Atomics.or(array, 1, 0b1000)).toBe(0b0101);
        expect(array).toEqual([0b1111, 0b1101]);

        expect(Atomics.or(array, 1, 0b0010)).toBe(0b1101);
        expect(array).toEqual([0b1111, 0b1111]);
    });
});

test("basic functionality (BigInt)", () => {
    [BigInt64Array, BigUint64Array].forEach(ArrayType => {
        const array = new ArrayType(2);
        array[0] = 0b0000n;
        array[1] = 0b0101n;

        expect(Atomics.or(array, 0, 0b0000n)).toBe(0b0000n);
        expect(array).toEqual([0b0000n, 0b0101n]);

        expect(Atomics.or(array, 0, 0b1111n)).toBe(0b0000n);
        expect(array).toEqual([0b1111n, 0b0101n]);

        expect(Atomics.or(array, 1, 0b0101n)).toBe(0b0101n);
        expect(array).toEqual([0b1111n, 0b0101n]);

        expect(Atomics.or(array, 1, 0b1000n)).toBe(0b0101n);
        expect(array).toEqual([0b1111n, 0b1101n]);

        expect(Atomics.or(array, 1, 0b0010n)).toBe(0b1101n);
        expect(array).toEqual([0b1111n, 0b1111n]);
    });
});
