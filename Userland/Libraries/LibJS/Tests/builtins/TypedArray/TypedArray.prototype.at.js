// Update when more typed arrays get added
const TYPED_ARRAYS = [
    Uint8Array,
    Uint16Array,
    Uint32Array,
    Int8Array,
    Int16Array,
    Int32Array,
    Float32Array,
    Float64Array,
];

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
});
