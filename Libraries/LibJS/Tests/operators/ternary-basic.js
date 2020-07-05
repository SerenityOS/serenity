load("test-common.js");

try {
  var x = 1;

  assert(x === 1 ? true : false);
  assert((x ? x : 0) === x);
  assert(1 < 2 ? true : false);
  assert((0 ? 1 : 1 ? 10 : 20) === 10);
  assert((0 ? (1 ? 1 : 10) : 20) === 20);

  var o = {};
  o.f = true;
  assert(o.f ? true : false);

  assert(1 ? o.f : null);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
