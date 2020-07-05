test("assignment to function call", () => {
  expect(() => {
    function foo() {}
    foo() = "foo";
  }).toThrowWithMessage(ReferenceError, "Invalid left-hand side in assignment");
});

test("assignment to inline function call", () => {
  expect(() => {
    (function () {})() = "foo";
  }).toThrowWithMessage(ReferenceError, "Invalid left-hand side in assignment");
});
