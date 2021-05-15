"use strict";

test("basic functionality", () => {
    [true, false, "foo", 123].forEach(primitive => {
        expect(() => {
            primitive.foo = "bar";
        }).toThrowWithMessage(
            TypeError,
            `Cannot set property 'foo' of ${typeof primitive} '${primitive}'`
        );
        expect(() => {
            primitive[Symbol.hasInstance] = 123;
        }).toThrowWithMessage(
            TypeError,
            `Cannot set property 'Symbol(Symbol.hasInstance)' of ${typeof primitive} '${primitive}'`
        );
    });
    [null, undefined].forEach(primitive => {
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
});
