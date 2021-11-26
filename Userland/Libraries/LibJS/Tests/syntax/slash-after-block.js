test("slash token resolution in lexer", () => {
    expect(`{ blah.blah; }\n/foo/`).toEval();
    expect("``/foo/").not.toEval();
    expect("1/foo/").not.toEval();
    expect("1/foo").toEval();

    expect("{} /foo/").toEval();
    expect("{} /=/").toEval();
    expect("{} /=a/").toEval();
    expect("{} /* */ /=a/").toEval();
    expect("{} /* /a/ */ /=a/").toEval();

    expect("(function () {} / 1)").toEval();
    expect("(function () {} / 1)").toEval();

    expect("+a++ / 1").toEval();
    expect("+a-- / 1").toEval();
    expect("a.in / b").toEval();
    expect("a.instanceof / b").toEval();
    expect("class A { #name; d = a.#name / b; }").toEval();

    // FIXME: Even more 'reserved' words are valid however the cases below do still need to pass.
    //expect("a.void / b").toEval();
    expect("void / b/").toEval();
});
