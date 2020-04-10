try {
  assert(typeof Number.prototype === "object");
  assert(Number.prototype.valueOf() === 0);

  console.log("PASS");
} catch (err) {
  console.log("FAIL: " + err);
}
