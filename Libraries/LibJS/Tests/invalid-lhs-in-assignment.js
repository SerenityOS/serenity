test("assignment to function call", () => {
    expect(() => {
        function foo() {}
        foo() = "foo";
    }).toThrowWithMessage(ReferenceError, "Invalid left-hand side in assignment");
});

test("assignment to function call in strict mode", () => {
    expect("'use strict'; foo() = 'foo'").not.toEval();
});

test("assignment to inline function call", () => {
    expect(() => {
        (function () {})() = "foo";
    }).toThrowWithMessage(ReferenceError, "Invalid left-hand side in assignment");
});

test("assignment to invalid LHS is syntax error", () => {
    expect("1 += 1").not.toEval();
    expect("1 -= 1").not.toEval();
    expect("1 *= 1").not.toEval();
    expect("1 /= 1").not.toEval();
    expect("1 %= 1").not.toEval();
    expect("1 **= 1").not.toEval();
    expect("1 &= 1").not.toEval();
    expect("1 |= 1").not.toEval();
    expect("1 ^= 1").not.toEval();
    expect("1 <<= 1").not.toEval();
    expect("1 >>= 1").not.toEval();
    expect("1 >>>= 1").not.toEval();
    expect("1 = 1").not.toEval();
});
