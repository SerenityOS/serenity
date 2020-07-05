load("test-common.js");

try {
  assert(BigInt.prototype.toLocaleString.length === 0);

  assertThrowsError(
    () => {
      BigInt.prototype.toLocaleString.call("foo");
    },
    {
      error: TypeError,
      message: "Not a BigInt object",
    }
  );

  assert(BigInt(123).toLocaleString() === "123");

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
