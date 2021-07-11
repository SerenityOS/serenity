describe("duplicated variable declarations should throw", () => {
    test("given two declarations in the same statement", () => {
        expect("let a, a;").not.toEval();
        expect("const a, a;").not.toEval();

        expect("var a, a;").toEval();
    });

    test("fail to parse if repeated over multiple statements", () => {
        expect("let a; var a;").not.toEval();
        expect("let b, a; var c, a;").not.toEval();
        expect("const a; var a;").not.toEval();
        expect("const b, a; var c, a;").not.toEval();
    });

    test.skip("should fail to parse even if variable first declared with var", () => {
        expect.skip("var a; let a;").toEval();
        expect.skip("var a; let b, a;").toEval();
        expect.skip("var a; const a;").toEval();
        expect.skip("var a; const b, a;").toEval();
    });

    test.skip("special cases with for loops", () => {
        expect("for (var a;;) { let a; }").toEval();
        expect("for (let a;;) { let a; }").toEval();
        expect("for (var a;;) { var a; }").toEval();

        expect("for (let a;;) { var a; }").not.toEval();
    });
});
