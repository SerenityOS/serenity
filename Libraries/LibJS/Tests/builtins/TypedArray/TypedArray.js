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

const getTypedArrayConstructor = () => Object.getPrototypeOf(TYPED_ARRAYS[0]);

test("basic functionality", () => {
    const TypedArray = getTypedArrayConstructor();
    expect(TypedArray).toHaveLength(0);
    expect(TypedArray.name).toBe("TypedArray");
    expect(TypedArray.prototype.constructor).toBe(TypedArray);
    TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.constructor).toBe(T);
    });
    const FunctionPrototype = Object.getPrototypeOf(() => {});
    expect(Object.getPrototypeOf(TypedArray)).toBe(FunctionPrototype);
});

test("typed array constructors must be invoked with 'new'", () => {
    TYPED_ARRAYS.forEach(T => {
        expect(() => {
            T();
        }).toThrowWithMessage(TypeError, `${T.name} constructor must be called with 'new'`);
    });
});

test("typed array constructors have TypedArray as prototype", () => {
    const TypedArray = getTypedArrayConstructor();
    TYPED_ARRAYS.forEach(T => {
        expect(Object.getPrototypeOf(T)).toBe(TypedArray);
    });
});

test("typed array prototypes have TypedArray.prototype as prototype", () => {
    const TypedArray = getTypedArrayConstructor();
    TYPED_ARRAYS.forEach(T => {
        const TPrototype = Object.getPrototypeOf(new T());
        expect(Object.getPrototypeOf(TPrototype)).toBe(TypedArray.prototype);
    });
});

test("typed arrays inherit from TypedArray", () => {
    const TypedArray = getTypedArrayConstructor();
    TYPED_ARRAYS.forEach(T => {
        expect(new T()).toBeInstanceOf(TypedArray);
    });
});

test("typed array can share the same ArrayBuffer", () => {
    const arrayBuffer = new ArrayBuffer(2);
    const uint8Array = new Uint8Array(arrayBuffer);
    const uint16Array = new Uint16Array(arrayBuffer);
    expect(uint8Array[0]).toBe(0);
    expect(uint8Array[1]).toBe(0);
    expect(uint16Array[0]).toBe(0);
    expect(uint16Array[1]).toBeUndefined();
    uint16Array[0] = 54321;
    expect(uint8Array[0]).toBe(0x31);
    expect(uint8Array[1]).toBe(0xd4);
    expect(uint16Array[0]).toBe(54321);
    expect(uint16Array[1]).toBeUndefined();
});

test("typed array from ArrayBuffer with custom length and offset", () => {
    const arrayBuffer = new ArrayBuffer(10);
    const uint8ArrayAll = new Uint8Array(arrayBuffer);
    const uint16ArrayPartial = new Uint16Array(arrayBuffer, 2, 4);
    // Affects two bytes of the buffer, beginning at offset
    uint16ArrayPartial[0] = 52651;
    // Out of relative bounds, doesn't affect buffer
    uint16ArrayPartial[4] = 123;
    expect(uint8ArrayAll[0]).toBe(0);
    expect(uint8ArrayAll[1]).toBe(0);
    expect(uint8ArrayAll[2]).toBe(0xab);
    expect(uint8ArrayAll[3]).toBe(0xcd);
    expect(uint8ArrayAll[5]).toBe(0);
    expect(uint8ArrayAll[6]).toBe(0);
    expect(uint8ArrayAll[7]).toBe(0);
    expect(uint8ArrayAll[8]).toBe(0);
    expect(uint8ArrayAll[9]).toBe(0);
});

test("typed array from ArrayBuffer errors", () => {
    expect(() => {
        new Uint16Array(new ArrayBuffer(1));
    }).toThrowWithMessage(
        RangeError,
        "Invalid buffer length for Uint16Array: must be a multiple of 2, got 1"
    );

    expect(() => {
        new Uint16Array(new ArrayBuffer(), 1);
    }).toThrowWithMessage(
        RangeError,
        "Invalid byte offset for Uint16Array: must be a multiple of 2, got 1"
    );

    expect(() => {
        new Uint16Array(new ArrayBuffer(), 2);
    }).toThrowWithMessage(
        RangeError,
        "Typed array byte offset 2 is out of range for buffer with length 0"
    );

    expect(() => {
        new Uint16Array(new ArrayBuffer(7), 2, 3);
    }).toThrowWithMessage(
        RangeError,
        "Typed array range 2:8 is out of range for buffer with length 7"
    );
});

test("TypedArray is not exposed on the global object", () => {
    expect(globalThis.TypedArray).toBeUndefined();
});

test("TypedArray is abstract", () => {
    const TypedArray = getTypedArrayConstructor();
    expect(() => {
        TypedArray();
    }).toThrowWithMessage(TypeError, "Abstract class TypedArray cannot be constructed directly");
    expect(() => {
        new TypedArray();
    }).toThrowWithMessage(TypeError, "Abstract class TypedArray cannot be constructed directly");
});
