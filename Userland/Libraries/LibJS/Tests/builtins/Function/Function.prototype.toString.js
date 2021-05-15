test("basic functionality", () => {
    expect(function () {}.toString()).toBe("function () {\n  ???\n}");
    expect(function (foo) {}.toString()).toBe("function (foo) {\n  ???\n}");
    expect(function (foo, bar, baz) {}.toString()).toBe("function (foo, bar, baz) {\n  ???\n}");
    expect(
        function (foo, bar, baz) {
            if (foo) {
                return baz;
            } else if (bar) {
                return foo;
            }
            return bar + 42;
        }.toString()
    ).toBe("function (foo, bar, baz) {\n  ???\n}");
    expect(console.debug.toString()).toBe("function debug() {\n  [native code]\n}");
    expect(Function.toString()).toBe("function Function() {\n  [native code]\n}");
});
