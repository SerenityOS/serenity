describe("errors", () => {
    test("called on non-ArrayBuffer object", () => {
        expect(() => {
            ArrayBuffer.prototype.resize(10);
        }).toThrowWithMessage(TypeError, "Not an object of type ArrayBuffer");
    });

    test("fixed buffer", () => {
        let buffer = new ArrayBuffer(5);
        detachArrayBuffer(buffer);

        expect(() => {
            buffer.resize(10);
        }).toThrowWithMessage(TypeError, "ArrayBuffer is not resizable");
    });

    test("detached buffer", () => {
        let buffer = new ArrayBuffer(5, { maxByteLength: 10 });
        detachArrayBuffer(buffer);

        expect(() => {
            buffer.resize(10);
        }).toThrowWithMessage(TypeError, "ArrayBuffer is detached");
    });

    test("invalid new byte length", () => {
        let buffer = new ArrayBuffer(5, { maxByteLength: 10 });

        expect(() => {
            buffer.resize(-1);
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    });

    test("new byte length exceeds maximum size", () => {
        let buffer = new ArrayBuffer(5, { maxByteLength: 10 });

        expect(() => {
            buffer.resize(11);
        }).toThrowWithMessage(
            RangeError,
            "ArrayBuffer byte length of 11 exceeds the max byte length of 10"
        );
    });
});

describe("normal behavior", () => {
    test("resizable buffer", () => {
        let buffer = new ArrayBuffer(5, { maxByteLength: 10 });
        expect(buffer.byteLength).toBe(5);
        expect(buffer.maxByteLength).toBe(10);

        for (let i = 0; i <= buffer.maxByteLength; ++i) {
            buffer.resize(i);
            expect(buffer.byteLength).toBe(i);
        }
    });
});
