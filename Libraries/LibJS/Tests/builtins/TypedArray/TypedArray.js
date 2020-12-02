// Update when more typed arrays get added
const TYPED_ARRAYS = [Uint8Array, Uint16Array, Uint32Array, Int8Array, Int16Array, Int32Array];

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
