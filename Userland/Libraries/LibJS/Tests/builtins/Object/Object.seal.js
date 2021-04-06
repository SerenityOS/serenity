test("length is 1", () => {
    expect(Object.seal).toHaveLength(1);
});

describe("normal behavior", () => {
    test("returns given argument", () => {
        const o = {};
        expect(Object.seal(42)).toBe(42);
        expect(Object.seal("foobar")).toBe("foobar");
        expect(Object.seal(o)).toBe(o);
    });

    test("prevents addition of new properties", () => {
        const o = {};
        expect(o.foo).toBeUndefined();
        Object.seal(o);
        o.foo = "bar";
        expect(o.foo).toBeUndefined();
    });

    test("prevents deletion of existing properties", () => {
        const o = { foo: "bar" };
        expect(o.foo).toBe("bar");
        Object.seal(o);
        delete o.foo;
        expect(o.foo).toBe("bar");
    });

    test("prevents changing attributes of existing properties", () => {
        const o = { foo: "bar" };
        Object.seal(o);
        // FIXME: These don't change anything and should not throw!
        // expect(Object.defineProperty(o, "foo", {})).toBe(o);
        // expect(Object.defineProperty(o, "foo", { configurable: false })).toBe(o);
        expect(() => {
            Object.defineProperty(o, "foo", { configurable: true });
        }).toThrowWithMessage(
            TypeError,
            "Cannot change attributes of non-configurable property 'foo'"
        );
    });

    test("doesn't prevent changing value of existing properties", () => {
        const o = { foo: "bar" };
        expect(o.foo).toBe("bar");
        Object.seal(o);
        o.foo = "baz";
        expect(o.foo).toBe("baz");
    });
});
