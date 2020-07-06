describe("[[Set]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect((new Proxy({}, { set: undefined }).foo = 1)).toBe(1);
        expect((new Proxy({}, { set: null }).foo = 1)).toBe(1);
        expect((new Proxy({}, {}).foo = 1)).toBe(1);
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            set(target, prop, value, receiver) {
                expect(target).toBe(o);
                expect(prop).toBe("foo");
                expect(value).toBe(10);
                expect(receiver).toBe(p);
                return true;
            },
        });

        p.foo = 10;
    });

    test("conditional return value", () => {
        let p = new Proxy(
            {},
            {
                set(target, prop, value) {
                    if (target[prop] === value) {
                        target[prop] *= 2;
                    } else {
                        target[prop] = value;
                    }
                },
            }
        );

        p.foo = 10;
        expect(p.foo).toBe(10);
        p.foo = 10;
        expect(p.foo).toBe(20);
        p.foo = 10;
        expect(p.foo).toBe(10);
    });
});

describe("[[Set]] invariants", () => {
    test("cannot return true for a non-configurable, non-writable property", () => {
        let o = {};
        Object.defineProperty(o, "foo", { value: 10 });

        let p = new Proxy(o, {
            set() {
                return true;
            },
        });

        expect(() => {
            p.foo = 12;
        }).toThrowWithMessage(
            TypeError,
            "Proxy handler's set trap violates invariant: cannot return true for a property on the target which is a non-configurable, non-writable own data property"
        );
    });

    test("cannot return true for a non-configurable accessor property with no setter", () => {
        let o = {};
        Object.defineProperty(o, "foo", { get() {} });

        let p = new Proxy(o, {
            set() {
                return true;
            },
        });

        expect(() => {
            p.foo = 12;
        }).toThrowWithMessage(
            TypeError,
            "Proxy handler's set trap violates invariant: cannot return true for a property on the target which is a non-configurable own accessor property with an undefined set attribute"
        );
    });
});
