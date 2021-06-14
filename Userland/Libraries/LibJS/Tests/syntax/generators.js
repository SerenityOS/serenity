describe("parsing freestanding generators", () => {
    test("simple", () => {
        expect(`function* foo() {}`).toEval();
        expect(`function *foo() {}`).toEval();
        expect(`function
            *foo() {}`).toEval();
    });
    test("yield expression", () => {
        expect(`function* foo() { yield; }`).toEval();
        expect(`function* foo() { yield (yield); }`).toEval();
        expect(`function* foo() { yield (yield foo); }`).toEval();
        expect(`function foo() { yield; }`).toEval();
        expect(`function foo() { yield 3; }`).not.toEval();
    });
    test.skip("yield-from expression", () => {
        expect(`function* foo() { yield *bar; }`).toEval();
        expect(`function* foo() { yield *(yield); }`).toEval();
        expect(`function* foo() { yield
            *bar; }`).not.toEval();
        expect(`function foo() { yield
            *bar; }`).toEval();
    });
});
