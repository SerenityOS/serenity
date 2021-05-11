describe("errors", () => {
    test("invalid pattern", () => {
        expect(() => {
            RegExp("[");
        }).toThrowWithMessage(
            SyntaxError,
            "RegExp compile error: Error during parsing of regular expression:"
        );
    });

    test("invalid flag", () => {
        expect(() => {
            RegExp("", "x");
        }).toThrowWithMessage(SyntaxError, "Invalid RegExp flag 'x'");
    });

    test("repeated flag", () => {
        expect(() => {
            RegExp("", "gg");
        }).toThrowWithMessage(SyntaxError, "Repeated RegExp flag 'g'");
    });
});

test("basic functionality", () => {
    expect(RegExp().toString()).toBe("/(?:)/");
    expect(RegExp(undefined).toString()).toBe("/(?:)/");
    expect(RegExp("foo").toString()).toBe("/foo/");
    expect(RegExp("foo", undefined).toString()).toBe("/foo/");
    expect(RegExp("foo", "g").toString()).toBe("/foo/g");
    expect(RegExp(undefined, "g").toString()).toBe("/(?:)/g");
});
