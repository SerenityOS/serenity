test("basic functionality", () => {
    expect(ArrayBuffer).toHaveLength(1);
    expect(ArrayBuffer.name).toBe("ArrayBuffer");
    expect(ArrayBuffer.prototype.constructor).toBe(ArrayBuffer);
    expect(new ArrayBuffer()).toBeInstanceOf(ArrayBuffer);
    expect(typeof new ArrayBuffer()).toBe("object");
});

test("ArrayBuffer constructor must be invoked with 'new'", () => {
    expect(() => {
        ArrayBuffer();
    }).toThrowWithMessage(TypeError, "ArrayBuffer constructor must be called with 'new'");
});

describe("resizable array buffer", () => {
    test("construct with options", () => {
        expect(new ArrayBuffer(5, { maxByteLength: 5 })).toBeInstanceOf(ArrayBuffer);
    });

    test("resizable when provided max byte length", () => {
        expect(new ArrayBuffer(1).resizable).toEqual(false);
        expect(new ArrayBuffer(1, {}).resizable).toEqual(false);
        expect(new ArrayBuffer(1, { maxByteLength: undefined }).resizable).toEqual(false);
        expect(new ArrayBuffer(1, { maxByteLength: 1 }).resizable).toEqual(true);
    });

    test("byte length must be shorter than max byte length", () => {
        expect(() => {
            new ArrayBuffer(1, { maxByteLength: 0 });
        }).toThrowWithMessage(RangeError, "Byte length exceeds maxByteLength option");
    });

    test("max byte length cannot be too large", () => {
        expect(() => {
            new ArrayBuffer(0, { maxByteLength: 9007199254740992 });
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    });

    test("max byte length cannot be negative", () => {
        expect(() => {
            new ArrayBuffer(0, { maxByteLength: -1 });
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    });

    test("invalid max byte length object", () => {
        expect(() => {
            new ArrayBuffer(0, {
                maxByteLength: {
                    toString: function () {
                        return {};
                    },
                    valueOf: function () {
                        return {};
                    },
                },
            });
        }).toThrowWithMessage(TypeError, "Cannot convert object to number");

        expect(() => {
            new ArrayBuffer(0, {
                get maxByteLength() {
                    throw "Exception";
                },
            });
        }).toThrow();
    });
});
