load("test-common.js");

try {
  assert(BigInt.asUintN.length === 2);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
