test("basic eval() functionality", () => {
    expect(eval("1 + 2")).toBe(3);

    function foo(a) {
        var x = 5;
        eval("x += a");
        return x;
    }
    expect(foo(7)).toBe(12);
});

test("returns value of last value-producing statement", () => {
    // See https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
    expect(eval("1;;;;;")).toBe(1);
    expect(eval("1;{}")).toBe(1);
    expect(eval("1;var a;")).toBe(1);
});

test("syntax error", () => {
    expect(() => {
        eval("{");
    }).toThrowWithMessage(
        SyntaxError,
        "Unexpected token Eof. Expected CurlyClose (line: 1, column: 2)"
    );
});
