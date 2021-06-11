describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Error).toHaveLength(1);
        expect(EvalError).toHaveLength(1);
        expect(RangeError).toHaveLength(1);
        expect(ReferenceError).toHaveLength(1);
        expect(SyntaxError).toHaveLength(1);
        expect(TypeError).toHaveLength(1);
    });

    test("name matches constructor name", () => {
        expect(Error.name).toBe("Error");
        expect(EvalError.name).toBe("EvalError");
        expect(RangeError.name).toBe("RangeError");
        expect(ReferenceError.name).toBe("ReferenceError");
        expect(SyntaxError.name).toBe("SyntaxError");
        expect(TypeError.name).toBe("TypeError");
    });

    test("basic functionality", () => {
        expect(Error()).toBeInstanceOf(Error);
        expect(new Error()).toBeInstanceOf(Error);
        expect(EvalError()).toBeInstanceOf(EvalError);
        expect(new EvalError()).toBeInstanceOf(EvalError);
        expect(RangeError()).toBeInstanceOf(RangeError);
        expect(new RangeError()).toBeInstanceOf(RangeError);
        expect(ReferenceError()).toBeInstanceOf(ReferenceError);
        expect(new ReferenceError()).toBeInstanceOf(ReferenceError);
        expect(SyntaxError()).toBeInstanceOf(SyntaxError);
        expect(new SyntaxError()).toBeInstanceOf(SyntaxError);
        expect(TypeError()).toBeInstanceOf(TypeError);
        expect(new TypeError()).toBeInstanceOf(TypeError);
    });
});
