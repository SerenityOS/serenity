try {
  assert(Boolean.length === 1);
  assert(typeof new Boolean() === "object");
  assert(new Boolean().valueOf() === false);

  var foo = new Boolean(true);
  var bar = new Boolean(true);

  assert(foo !== bar);
  assert(foo.valueOf() === bar.valueOf());

  assert(new Boolean(true).toString() === "true");
  assert(new Boolean(false).toString() === "false");

  assert(typeof Boolean() === "boolean");
  assert(typeof Boolean(true) === "boolean");

  assert(Boolean() === false);
  assert(Boolean(false) === false);
  assert(Boolean(null) === false);
  assert(Boolean(undefined) === false);
  assert(Boolean(NaN) === false);
  assert(Boolean("") === false);
  assert(Boolean(0.0) === false);
  assert(Boolean(-0.0) === false);
  assert(Boolean(true) === true);
  assert(Boolean("0") === true);
  assert(Boolean({}) === true);
  assert(Boolean([]) === true);
  assert(Boolean(1)) === true;

  console.log("PASS");
} catch (err) {
  console.log("FAIL: " + err);
}
