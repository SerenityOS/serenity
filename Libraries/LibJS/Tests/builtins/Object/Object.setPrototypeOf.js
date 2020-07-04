describe("correct behavior", () => {
    test("length", () => {
        expect(Object.setPrototypeOf).toHaveLength(2);
    });

    test("basic functionality", () => {
        let o = {};
        let p = {};
        expect(Object.setPrototypeOf(o, p)).toBe(o);
        expect(Object.getPrototypeOf(o)).toBe(p);
    });
});

describe("errors", () => {
    test("requires two arguments", () => {
        expect(() => {
            Object.setPrototypeOf();
        }).toThrowWithMessage(TypeError, "Object.setPrototypeOf requires at least two arguments");

        expect(() => {
            Object.setPrototypeOf({});
        }).toThrowWithMessage(TypeError, "Object.setPrototypeOf requires at least two arguments");
    });

    test("prototype must be an object", () => {
        expect(() => {
            Object.setPrototypeOf({}, "foo");
        }).toThrowWithMessage(TypeError, "Prototype must be an object or null");
    });

    test("non-extensible target", () => {
        let o = {};
        let p = {};
        Object.setPrototypeOf(o, p);
        Object.preventExtensions(o);

        expect(() => {
            Object.setPrototypeOf(o, {});
        }).toThrowWithMessage(TypeError, "Object's [[SetPrototypeOf]] method returned false");

        expect(Object.setPrototypeOf(o, p)).toBe(o);
    });
});
