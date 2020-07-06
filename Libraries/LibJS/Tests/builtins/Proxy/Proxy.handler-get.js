describe("[[Get]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect(new Proxy({}, { get: undefined }).foo).toBeUndefined();
        expect(new Proxy({}, { get: null }).foo).toBeUndefined();
        expect(new Proxy({}, {}).foo).toBeUndefined();
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            get(target, property, receiver) {
                expect(target).toBe(o);
                expect(property).toBe("foo");
                expect(receiver).toBe(p);
            },
        });

        p.foo;
    });

    test("conditional return", () => {
        let o = { foo: 1 };
        let p = new Proxy(o, {
            get(target, property, receiver) {
                if (property === "bar") {
                    return 2;
                } else if (property === "baz") {
                    return receiver.qux;
                } else if (property === "qux") {
                    return 3;
                }
                return target[property];
            },
        });

        expect(p.foo).toBe(1);
        expect(p.bar).toBe(2);
        expect(p.baz).toBe(3);
        expect(p.qux).toBe(3);
        expect(p.test).toBeUndefined();
    });
});

describe("[[Get]] invariants", () => {
    test("returned value must match the target property value if the property is non-configurable and non-writable", () => {
        let o = {};
        Object.defineProperty(o, "foo", { value: 5, configurable: false, writable: true });
        Object.defineProperty(o, "bar", { value: 10, configurable: false, writable: false });

        let p = new Proxy(o, {
            get() {
                return 8;
            },
        });

        expect(p.foo).toBe(8);
        expect(() => {
            p.bar;
        }).toThrowWithMessage(
            TypeError,
            "Proxy handler's get trap violates invariant: the returned value must match the value on the target if the property exists on the target as a non-writable, non-configurable own data property"
        );
    });

    test("returned value must be undefined if the property is a non-configurable accessor with no getter", () => {
        let o = {};
        Object.defineProperty(o, "foo", { configurable: false, set(_) {} });

        let p = new Proxy(o, {
            get() {
                return 8;
            },
        });

        expect(() => {
            p.foo;
        }).toThrowWithMessage(
            TypeError,
            "Proxy handler's get trap violates invariant: the returned value must be undefined if the property exists on the target as a non-configurable accessor property with an undefined get attribute"
        );
    });
});
