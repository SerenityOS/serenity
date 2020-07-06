"use strict";

test("basic functionality", () => {
    [true, false, "foo", 123].forEach(primitive => {
        expect(() => {
            primitive.foo = "bar";
        }).toThrowWithMessage(TypeError, "Cannot assign property foo to primitive value");
    });
});
