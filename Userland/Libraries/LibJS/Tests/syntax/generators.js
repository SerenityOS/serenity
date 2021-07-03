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
    test("yield-from expression", () => {
        expect(`function* foo() { yield *bar; }`).toEval();
        expect(`function* foo() { yield *(yield); }`).toEval();
        expect(`function* foo() { yield
            *bar; }`).not.toEval();
        expect(`function foo() { yield
            *bar; }`).toEval();
    });
});

describe("parsing object literal generator functions", () => {
    test("simple", () => {
        expect(`x = { *foo() { } }`).toEval();
        expect(`x = { * foo() { } }`).toEval();
        expect(`x = { *
                foo() { } }`).toEval();
    });
    test("yield", () => {
        expect(`x = { foo() { yield; } }`).toEval();
        expect(`x = { *foo() { yield; } }`).toEval();
        expect(`x = { *foo() { yield 42; } }`).toEval();
        expect(`x = { foo() { yield 42; } }`).not.toEval();
        expect(`x = { *foo() { yield (yield); } }`).toEval();
        expect(`x = { *
                foo() { yield (yield); } }`).toEval();
    });
});
