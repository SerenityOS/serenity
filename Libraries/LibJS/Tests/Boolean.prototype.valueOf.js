try {
  var foo = true;
  assert(foo.valueOf() === true);
  assert(true.valueOf() === true);

  assert(Boolean.prototype.valueOf.call(true) === true);
  assert(Boolean.prototype.valueOf.call(false) === false);

  let error = null;
  try {
    Boolean.prototype.valueOf.call("foo");
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
