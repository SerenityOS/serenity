load("test-common.js");

try {
  assert(typeof "foo" === "string");
  assert(!(typeof "foo" !== "string"));
  assert(typeof (1 + 2) === "number");
  assert(typeof {} === "object");
  assert(typeof null === "object");
  assert(typeof undefined === "undefined");

  var iExist = 1;
  assert(typeof iExist === "number");
  assert(typeof iDontExist === "undefined");

  var calls = 0;
  Object.defineProperty(globalThis, "foo", {
    get() {
      calls++;
    },
  });
  assert(typeof foo === "undefined");
  assert(calls === 1);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
