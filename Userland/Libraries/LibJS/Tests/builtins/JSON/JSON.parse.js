test("basic functionality", () => {
    expect(JSON.parse).toHaveLength(2);

    const properties = [
        ["5", 5],
        ["null", null],
        ["true", true],
        ["false", false],
        ['"test"', "test"],
        ['[1,2,"foo"]', [1, 2, "foo"]],
        ['{"foo":1,"bar":"baz"}', { foo: 1, bar: "baz" }],
    ];

    properties.forEach(testCase => {
        expect(JSON.parse(testCase[0])).toEqual(testCase[1]);
    });
});

test("syntax errors", () => {
    [
        undefined,
        NaN,
        -NaN,
        Infinity,
        -Infinity,
        '{ "foo" }',
        '{ foo: "bar" }',
        "[1,2,3,]",
        "[1,2,3, ]",
        '{ "foo": "bar",}',
        '{ "foo": "bar", }',
    ].forEach(test => {
        expect(() => {
            JSON.parse(test);
        }).toThrow(SyntaxError);
    });
});

test("negative zero", () => {
    ["-0", " \n-0", "-0  \t", "\n\t -0\n   ", "-0.0"].forEach(testCase => {
        expect(JSON.parse(testCase)).toEqual(-0.0);
    });

    expect(JSON.parse(-0)).toEqual(0);
});
