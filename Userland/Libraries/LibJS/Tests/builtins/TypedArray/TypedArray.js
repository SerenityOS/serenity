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

const getTypedArrayConstructor = () => Object.getPrototypeOf(TYPED_ARRAYS[0]);

test("basic functionality", () => {
    const TypedArray = getTypedArrayConstructor();
    expect(TypedArray).toHaveLength(0);
    expect(TypedArray.name).toBe("TypedArray");
    expect(TypedArray.prototype.constructor).toBe(TypedArray);
    TYPED_ARRAYS.forEach(T => {
        expect(T.prototype.constructor).toBe(T);
    });
    BIGINT_TYPED_ARRAYS.forEach(T => {
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
    BIGINT_TYPED_ARRAYS.forEach(T => {
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
    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(Object.getPrototypeOf(T)).toBe(TypedArray);
    });
});

test("typed array prototypes have TypedArray.prototype as prototype", () => {
    const TypedArray = getTypedArrayConstructor();
    TYPED_ARRAYS.forEach(T => {
        const TPrototype = Object.getPrototypeOf(new T());
        expect(Object.getPrototypeOf(TPrototype)).toBe(TypedArray.prototype);
    });
    BIGINT_TYPED_ARRAYS.forEach(T => {
        const TPrototype = Object.getPrototypeOf(new T());
        expect(Object.getPrototypeOf(TPrototype)).toBe(TypedArray.prototype);
    });
});

test("typed arrays inherit from TypedArray", () => {
    const TypedArray = getTypedArrayConstructor();
    TYPED_ARRAYS.forEach(T => {
        expect(new T()).toBeInstanceOf(TypedArray);
    });
    BIGINT_TYPED_ARRAYS.forEach(T => {
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

test("typed array from TypedArray", () => {
    const u8Array = new Uint8Array(3);
    u8Array[0] = 1;
    u8Array[1] = 2;
    u8Array[2] = 3;

    TYPED_ARRAYS.forEach(T => {
        const newTypedArray = new T(u8Array);
        expect(newTypedArray[0]).toBe(1);
        expect(newTypedArray[1]).toBe(2);
        expect(newTypedArray[2]).toBe(3);
    });

    const bigU64Array = new BigUint64Array(3);
    bigU64Array[0] = 1n;
    bigU64Array[1] = 2n;
    bigU64Array[2] = 3n;

    BIGINT_TYPED_ARRAYS.forEach(T => {
        expect(() => {
            const newTypedArray = new T(u8Array);
        }).toThrowWithMessage(TypeError, `Can't create ${T.name} from Uint8Array`);

        const newBigIntTypedArray = new T(bigU64Array);
        expect(newBigIntTypedArray[0]).toBe(1n);
        expect(newBigIntTypedArray[1]).toBe(2n);
        expect(newBigIntTypedArray[2]).toBe(3n);
    });
});

test("typed array from TypedArray element cast", () => {
    const u32Array = new Uint32Array(2);
    u32Array[0] = 0x100;
    u32Array[1] = 0xff;
    const u8Array = new Uint8Array(1);
    u8Array[0] = 0xff;

    const u32Expected = [
        [0, 0xff],
        [0xff, 0xff],
        [0x100, 0xff],
        [0x100, 0xff],
        [0, -1],
        [0x100, 0xff],
        [0x100, 0xff],
        [0x100, 0xff],
        [0x100, 0xff],
    ];
    const u8Expected = [0xff, 0xff, 0xff, 0xff, -1, 0xff, 0xff, 0xff, 0xff];

    TYPED_ARRAYS.forEach((T, i) => {
        const newArrFromU32 = new T(u32Array);
        expect(newArrFromU32[0]).toBe(u32Expected[i][0]);
        expect(newArrFromU32[1]).toBe(u32Expected[i][1]);

        const newArrFromU8 = new T(u8Array);
        expect(newArrFromU8[0]).toBe(u8Expected[i]);
    });
});

test("typed array created from TypedArray do not share buffer", () => {
    const u8Array = new Uint8Array(2);
    u8Array[0] = 1;
    u8Array[1] = 2;

    const u8Array2 = new Uint8Array(u8Array);
    u8Array2[0] = 3;
    u8Array2[1] = 4;

    expect(u8Array[0]).toBe(1);
    expect(u8Array[1]).toBe(2);

    const i32Array = new Int32Array(u8Array);
    expect(i32Array[0]).toBe(1);
    expect(i32Array[1]).toBe(2);

    i32Array[0] = 5;
    i32Array[1] = 6;
    expect(u8Array[0]).toBe(1);
    expect(u8Array[1]).toBe(2);
});

test("typed array from Array-Like", () => {
    TYPED_ARRAYS.forEach(T => {
        function func() {
            const newTypedArray = new T(arguments);
            expect(newTypedArray[0]).toBe(1);
            expect(newTypedArray[1]).toBe(2);
            expect(newTypedArray[2]).toBe(3);
        }
        func(1, 2, 3);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        function func() {
            const newTypedArray = new T(arguments);
            expect(newTypedArray[0]).toBe(1n);
            expect(newTypedArray[1]).toBe(2n);
            expect(newTypedArray[2]).toBe(3n);
        }
        func(1n, 2n, 3n);
    });
});

test("typed array from Iterable", () => {
    const from = new String("123");

    TYPED_ARRAYS.forEach(T => {
        const newTypedArray = new T(from);
        expect(newTypedArray[0]).toBe(1);
        expect(newTypedArray[1]).toBe(2);
        expect(newTypedArray[2]).toBe(3);
    });

    BIGINT_TYPED_ARRAYS.forEach(T => {
        const newTypedArray = new T(from);
        expect(newTypedArray[0]).toBe(1n);
        expect(newTypedArray[1]).toBe(2n);
        expect(newTypedArray[2]).toBe(3n);
    });
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
