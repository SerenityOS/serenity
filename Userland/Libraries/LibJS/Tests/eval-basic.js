test("basic eval() functionality", () => {
    expect(eval("1 + 2")).toBe(3);

    function foo(a) {
        var x = 5;
        eval("x += a");
        return x;
    }
    expect(foo(7)).toBe(12);
});
