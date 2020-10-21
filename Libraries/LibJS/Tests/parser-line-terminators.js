/*
These tests deliberately produce syntax errors to check what line the parser thinks we're on.

⚠ PLEASE MAKE SURE TO NOT LET YOUR EDITOR REMOVE THE LS/PS LINE TERMINATORS!
*/

test("LINE FEED is a line terminator", () => {
    expect(() => {
        Function("\n\n@");
    }).toThrowWithMessage(SyntaxError, "line: 3, column: 1");
});

test("CARRIAGE RETURN is a line terminator", () => {
    expect(() => {
        Function("\r\r@");
    }).toThrowWithMessage(SyntaxError, "line: 3, column: 1");
});

test("LINE SEPARATOR is a line terminator", () => {
    expect(() => {
        Function(`  @`);
    }).toThrowWithMessage(SyntaxError, "line: 3, column: 1");
});

test("PARAGRAPH SEPARATOR is a line terminator", () => {
    expect(() => {
        Function(`  @`);
    }).toThrowWithMessage(SyntaxError, "line: 3, column: 1");
});

test("CR LF is counted as only one line terminator", () => {
    expect(() => {
        Function("\r\n\r\n@");
    }).toThrowWithMessage(SyntaxError, "line: 3, column: 1");
});

test("LF/CR are not allowed in string literal", () => {
    expect(() => {
        Function(`"
        "`);
    }).toThrowWithMessage(SyntaxError, "Unexpected token UnterminatedStringLiteral");
});

test("LS/PS are allowed in string literal", () => {
    expect(`" "`).toEval();
    expect(`" "`).toEval();
});

test("line terminators can be mixed (but please don't)", () => {
    expect(() => {
        Function(`\r\ \r\n \n\r@`);
    }).toThrowWithMessage(SyntaxError, "line: 7, column: 1");
});
