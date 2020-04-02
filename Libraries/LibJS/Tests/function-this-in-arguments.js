function assert(x) { if (!x) throw 1; }

try {
  assert(typeof this === "object");
  assert(this === global);

  function Foo() {
    this.x = 5;
    assert(typeof this === "object");
    assert(this.x === 5);
  }

  new Foo();
  console.log("PASS");
} catch (err) {
  console.log("FAIL: " + err);
}
