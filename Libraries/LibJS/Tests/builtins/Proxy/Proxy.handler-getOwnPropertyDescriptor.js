describe("[Call][GetOwnProperty]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect(Object.getOwnPropertyDescriptor(new Proxy({}, { getOwnPropertyDescriptor: null }), "a")).toBeUndefined();
        expect(Object.getOwnPropertyDescriptor(new Proxy({}, { getOwnPropertyDescriptor: undefined }), "a")).toBeUndefined();
        expect(Object.getOwnPropertyDescriptor(new Proxy({}, {}), "a")).toBeUndefined();
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            getOwnPropertyDescriptor(target, property) {
                expect(target).toBe(o);
                expect(property).toBe("foo");
            }
        });

        Object.getOwnPropertyDescriptor(p, "foo");
    });

    test("conditional returned descriptor", () => {
        let o = { foo: "bar" };
        Object.defineProperty(o, "baz", { value: "qux", enumerable: false, configurable: true, writable: false });

        let p = new Proxy(o, {
            getOwnPropertyDescriptor(target, property) {
                if (property === "baz")
                    return Object.getOwnPropertyDescriptor(target, "baz");
                 return { value: target[property], enumerable: false, configurable: true, writable: true };
            }
        });

        let d = Object.getOwnPropertyDescriptor(p, "baz");
        expect(d.configurable).toBe(true);
        expect(d.enumerable).toBe(false);
        expect(d.writable).toBe(false);
        expect(d.value).toBe("qux");
        expect(d.get).toBeUndefined();
        expect(d.set).toBeUndefined();

        d = Object.getOwnPropertyDescriptor(p, "foo");
        expect(d.configurable).toBe(true);
        expect(d.enumerable).toBe(false);
        expect(d.writable).toBe(true);
        expect(d.value).toBe("bar");
        expect(d.get).toBeUndefined();
        expect(d.set).toBeUndefined();
    });
});

describe("[[GetOwnProperty]] invariants", () => {
    test("must return an object or undefined", () => {
        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy({}, {
                getOwnPropertyDescriptor() {
                    return 1;
                },
            }));
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: must return an object or undefined");
    });

    test("cannot return undefined for a non-configurable property", () => {
        let o = {};
        Object.defineProperty(o, "foo", { value: 10, configurable: false });

        let p = new Proxy(o, {
            getOwnPropertyDescriptor() {
                return undefined;
            },
        });

        expect(Object.getOwnPropertyDescriptor(p, "bar")).toBeUndefined();

        expect(() => {
            Object.getOwnPropertyDescriptor(p, "foo");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot return undefined for a property on the target which is a non-configurable property");
    });

    test("cannot return undefined for an existing property if the target is non-extensible", () => {
        let o = {};
        Object.defineProperty(o, "baz", { value: 20, configurable: true, writable: true, enumerable: true });
        Object.preventExtensions(o);

        let p = new Proxy(o, {
            getOwnPropertyDescriptor() {
                return undefined;
            },
        });

        expect(() => {
            Object.getOwnPropertyDescriptor(p, "baz");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot report a property as being undefined if it exists as an own property of the target and the target is non-extensible");
    });

    test("invalid property descriptors", () => {
        let o = {};
        Object.defineProperty(o, "v1", { value: 10, configurable: false });
        Object.defineProperty(o, "v2", { value: 10, configurable: false, enumerable: true });
        Object.defineProperty(o, "v3", { configurable: false, get() { return 1; } });
        Object.defineProperty(o, "v4", { value: 10, configurable: false, writable: false, enumerable: true });

        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy(o, {
                getOwnPropertyDescriptor() {
                    return { configurable: true };
                },
            }), "v1");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target");

        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy(o, {
                getOwnPropertyDescriptor() {
                    return { enumerable: false };
                },
            }), "v2");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target");

        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy(o, {
                getOwnPropertyDescriptor() {
                    return { value: 10 };
                },
            }), "v3");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target");

        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy(o, {
                getOwnPropertyDescriptor() {
                    return { value: 10, writable: true };
                },
            }), "v4");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: invalid property descriptor for existing property on the target");
    });

    test("cannot report a property as non-configurable if it does not exist or is non-configurable", () => {
        let o = {};
        Object.defineProperty(o, "v", { configurable: true });
        expect(() => {
            Object.getOwnPropertyDescriptor(new Proxy(o, {
                getOwnPropertyDescriptor() {
                    return { configurable: false };
                },
            }), "v");
        }).toThrowWithMessage(TypeError, "Proxy handler's getOwnPropertyDescriptor trap violates invariant: cannot report target's property as non-configurable if the property does not exist, or if it is configurable");
    });
});
