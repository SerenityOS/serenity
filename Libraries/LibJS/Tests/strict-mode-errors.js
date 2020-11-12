"use strict";

test("basic functionality", () => {
    [true, false, "foo", 123].forEach(primitive => {
        expect(() => {
            primitive.foo = "bar";
        }).toThrowWithMessage(TypeError, "Cannot assign property foo to primitive value");
        expect(() => {
            primitive[Symbol.hasInstance] = 123;
        }).toThrowWithMessage(TypeError, "Cannot assign property Symbol(Symbol.hasInstance) to primitive value");
    });
});
