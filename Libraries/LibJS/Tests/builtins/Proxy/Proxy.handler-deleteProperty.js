describe("[[Delete]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        expect(delete (new Proxy({}, { deleteProperty: undefined })).foo).toBe(true);
        expect(delete (new Proxy({}, { deleteProperty: null })).foo).toBe(true);
        expect(delete (new Proxy({}, {})).foo).toBe(true);
    });

    test("correct arguments supplied to trap", () => {
        let o = {};
        let p = new Proxy(o, {
            deleteProperty(target, property) {
                expect(target).toBe(o);
                expect(property).toBe("foo");
                return true;
            }
        });

        delete p.foo;
    });

    test("conditional deletion", () => {
        o = { foo: 1, bar: 2 };
        p = new Proxy(o, {
            deleteProperty(target, property) {
                if (property === "foo") {
                    delete target[property];
                    return true;
                }
                return false;
            }
        });

        expect(delete p.foo).toBe(true);
        expect(delete p.bar).toBe(false);

        expect(o.foo).toBe(undefined);
        expect(o.bar).toBe(2);
    });
});


describe("[[Delete]] invariants", () => {
    test("cannot report a non-configurable own property as deleted", () => {
        let o = {};
        Object.defineProperty(o, "foo", { configurable: false });
        let p = new Proxy(o, {
            deleteProperty() {
                return true;
            },
        });

        expect(() => {
            delete p.foo;
        }).toThrowWithMessage(TypeError, "Proxy handler's deleteProperty trap violates invariant: cannot report a non-configurable own property of the target as deleted");
    });
});
