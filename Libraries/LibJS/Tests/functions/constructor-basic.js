load("test-common.js");

try {
  function Foo() {
    this.x = 123;
  }

  assert(Foo.prototype.constructor === Foo);

  var foo = new Foo();
  assert(foo.constructor === Foo);
  assert(foo.x === 123);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
