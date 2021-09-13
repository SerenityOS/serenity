test("callee is evaluated before arguments", () => {
    function foo() {}
    const values = [];

    [foo][(values.push("callee"), 0)](values.push("args"));

    expect(values).toEqual(["callee", "args"]);
});

test("arguments are evaluated in order", () => {
    function foo() {}
    const values = [];

    foo(values.push("arg1"), values.push("arg2"), values.push("arg3"));

    expect(values).toEqual(["arg1", "arg2", "arg3"]);
});

test("arguments are evaluated before callee is checked for its type", () => {
    const values = [];

    expect(() => {
        "foo"(values.push("args"));
    }).toThrowWithMessage(TypeError, "foo is not a function");
    expect(values).toEqual(["args"]);

    expect(() => {
        "foo"(bar);
    }).toThrowWithMessage(ReferenceError, "'bar' is not defined");
});
