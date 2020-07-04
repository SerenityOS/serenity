describe("[[Has]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect("foo" in new Proxy({}, { has: null })).toBe(false);
        expect("foo" in new Proxy({}, { has: undefined})).toBe(false);
        expect("foo" in new Proxy({}, {})).toBe(false);
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            has(target, prop) {
                expect(target).toBe(o);
                expect(prop).toBe("foo");
                return true;
            }
        });

        "foo" in p;
    });

    test("conditional return", () => {
        let o = {};
        let p = new Proxy(o, {
            has(target, prop) {
                if (target.checkedFoo)
                    return true;
                if (prop === "foo")
                    target.checkedFoo = true;
                return false;
            }
        });

        expect("foo" in p).toBe(false);
        expect("foo" in p).toBe(true);
    });
});

describe("[[Has]] invariants", () => {
    test("cannot return false if the property exists and is non-configurable", () => {
        let o = {};
        Object.defineProperty(o, "foo", { configurable: false });

        p = new Proxy(o, {
            has() {
                return false;
            }
        });

        expect(() => {
            "foo" in p;
        }).toThrowWithMessage(TypeError, "Proxy handler's has trap violates invariant: a property cannot be reported as non-existent if it exists on the target as a non-configurable property");
    });

    test("cannot return false if the property exists and the target is non-extensible", () => {
        let o = {};
        Object.defineProperty(o, "foo", { value: 10, configurable: true });

        let p = new Proxy(o, {
            has() {
                return false;
            }
        });

        Object.preventExtensions(o);

        expect(() => {
            "foo" in p;
        }).toThrowWithMessage(TypeError, "Proxy handler's has trap violates invariant: a property cannot be reported as non-existent if it exists on the target and the target is non-extensible");
    });
});
