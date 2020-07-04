describe("[[IsExtensible]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect(Object.isExtensible(new Proxy({}, { isExtensible: null }))).toBe(true);
        expect(Object.isExtensible(new Proxy({}, { isExtensible: undefined }))).toBe(true);
        expect(Object.isExtensible(new Proxy({}, {}))).toBe(true);
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            isExtensible(target) {
                expect(target).toBe(o);
                return true;
            }
        });

        expect(Object.isExtensible(p)).toBe(true);
    });
});

describe("[[Call]] invariants", () => {
    test("return value must match the target's extensibility", () => {
        let o = {};
        Object.preventExtensions(o);

        let p = new Proxy(o, {
            isExtensible() {
                return true;
            },
        });

        expect(() => {
            Object.isExtensible(p);
        }).toThrowWithMessage(TypeError, "Proxy handler's isExtensible trap violates invariant: return value must match the target's extensibility");
    });
});
