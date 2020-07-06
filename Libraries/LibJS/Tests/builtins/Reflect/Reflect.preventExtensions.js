test("length is 1", () => {
    expect(Reflect.preventExtensions).toHaveLength(1);
});

describe("errors", () => {
    test("target must be an object", () => {
        [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
            expect(() => {
                Reflect.preventExtensions(value);
            }).toThrowWithMessage(
                TypeError,
                "First argument of Reflect.preventExtensions() must be an object"
            );
        });
    });
});

describe("normal behavior", () => {
    test("properties cannot be added", () => {
        var o = {};
        o.foo = "foo";
        expect(Reflect.preventExtensions(o)).toBeTrue();
        o.bar = "bar";
        expect(o.foo).toBe("foo");
        expect(o.bar).toBeUndefined();
    });

    test("property values can still be changed", () => {
        // FIXME: This doesn't work even though it should (the value remains unchanged)
        // var o = {};
        // o.foo = "foo";
        // expect(Reflect.preventExtensions(o)).toBeTrue();
        // o.foo = "bar";
        // expect(o.foo).toBe("bar");
    });
});
