try {
  assert(typeof Object.getPrototypeOf("") === "object");
  assert(Object.getPrototypeOf("").valueOf() === '');

  console.log("PASS");
} catch (err) {
  console.log("FAIL: " + err);
}
