describe("parsing freestanding async functions", () => {
    test("simple", () => {
        expect(`async function foo() {}`).toEval();
        expect(`async
        function foo() {}`).not.toEval();
    });
    test("await expression", () => {
        expect(`async function foo() { await bar(); }`).toEval();
        expect(`async function foo() { await; }`).not.toEval();
        expect(`function foo() { await bar(); }`).not.toEval();
        expect(`function foo() { await; }`).toEval();
    });
});

describe("parsing object literal async functions", () => {
    test("simple", () => {
        expect(`x = { async foo() { } }`).toEval();
        expect(`x = { async
                foo() { } }`).not.toEval();
    });
    test("await expression", () => {
        expect(`x = { foo() { await bar(); } }`).not.toEval();
        expect(`x = { foo() { await; } }`).toEval();
        expect(`x = { async foo() { await bar(); } }`).toEval();
        expect(`x = { async foo() { await; } }`).not.toEval();
    });
});

describe("parsing classes with async methods", () => {
    test("simple", () => {
        expect(`class Foo { async foo() {} }`).toEval();
        expect(`class Foo { static async foo() {} }`).toEval();
        expect(`class Foo { async foo() { await bar(); } }`).toEval();
        expect(`class Foo { async foo() { await; } }`).not.toEval();
        expect(`class Foo { async constructor() {} }`).not.toEval();
    });
});

test("function expression names equal to 'await'", () => {
    expect(`async function foo() { (function await() {}); }`).toEval();
    expect(`async function foo() { function await() {} }`).not.toEval();
});

test("basic functionality", () => {
    test("simple", () => {
        let executionValue = null;
        let resultValue = null;
        async function foo() {
            executionValue = "someValue";
            return "otherValue";
        }
        const returnValue = foo();
        expect(returnValue).toBeInstanceOf(Promise);
        returnValue.then(result => {
            resultValue = result;
        });
        runQueuedPromiseJobs();
        expect(executionValue).toBe("someValue");
        expect(resultValue).toBe("otherValue");
    });

    test("await", () => {
        let resultValue = null;
        async function foo() {
            return "someValue";
        }
        async function bar() {
            resultValue = await foo();
        }
        bar();
        runQueuedPromiseJobs();
        expect(resultValue).toBe("someValue");
    });
});
