load("test-common.js");

try {
  assert(BigInt.asIntN.length === 2);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
