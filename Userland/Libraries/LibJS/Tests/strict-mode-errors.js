"use strict";

test("basic functionality", () => {
    [true, false, "foo", 123].forEach(primitive => {
        expect(() => {
            primitive.foo = "bar";
        }).toThrowWithMessage(TypeError, `Cannot set property 'foo' of ${primitive}`);
        expect(() => {
            primitive[Symbol.hasInstance] = 123;
        }).toThrowWithMessage(
            TypeError,
            `Cannot set property 'Symbol(Symbol.hasInstance)' of ${primitive}`
        );
    });
    [null, undefined].forEach(primitive => {
        expect(() => {
            primitive.foo = "bar";
        }).toThrowWithMessage(TypeError, `${primitive} cannot be converted to an object`);
        expect(() => {
            primitive[Symbol.hasInstance] = 123;
        }).toThrowWithMessage(TypeError, `${primitive} cannot be converted to an object`);
    });
});
