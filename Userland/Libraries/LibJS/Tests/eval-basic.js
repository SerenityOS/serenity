test("basic eval() functionality", () => {
    expect(eval("1 + 2")).toBe(3);

    function foo(a) {
        var x = 5;
        eval("x += a");
        return x;
    }
    expect(foo(7)).toBe(12);
});

test("syntax error", () => {
    expect(() => {
        eval("{");
    }).toThrowWithMessage(
        SyntaxError,
        "Unexpected token Eof. Expected CurlyClose (line: 1, column: 2)"
    );
});
