load("test-common.js");

try {
    assert(JSON.parse.length === 2);

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
        assertDeepEquals(JSON.parse(testCase[0]), testCase[1]);
    });

    let syntaxErrors = [
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
    ];

    syntaxErrors.forEach(error => assertThrowsError(() => {
        JSON.parse(error);
    }, {
        error: SyntaxError,
    }));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
