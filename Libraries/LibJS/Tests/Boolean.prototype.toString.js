try {
  var foo = true;
  assert(foo.toString() === "true");
  assert(true.toString() === "true");

  assert(Boolean.prototype.toString.call(true) === "true");
  assert(Boolean.prototype.toString.call(false) === "false");

  let error = null;
  try {
    Boolean.prototype.toString.call("foo");
  } catch (err) {
    error = err;
  }

  assert(error instanceof Error);
  assert(error.name === "TypeError");
  assert(error.message === "Not a Boolean");

  console.log("PASS");
} catch (err) {
  console.log("FAIL: " + err);
}
