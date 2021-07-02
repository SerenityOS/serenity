test("slash token resolution in lexer", () => {
    expect(`{ blah.blah; }\n/foo/`).toEval();
    expect("``/foo/").not.toEval();
    expect("1/foo/").not.toEval();
    expect("1/foo").toEval();
});
