load("test-common.js");

try {
  function foo(...a) {
    assert(a instanceof Array);
    assert(a.length === 0);
  }
  foo();

  function foo1(...a) {
    assert(a instanceof Array);
    assert(a.length === 4);
    assert(a[0] === "foo");
    assert(a[1] === 123);
    assert(a[2] === undefined);
    assert(a[3].foo === "bar");
  }
  foo1("foo", 123, undefined, { foo: "bar" });

  function foo2(a, b, ...c) {
    assert(a === "foo");
    assert(b === 123);
    assert(c instanceof Array);
    assert(c.length === 0);
  }
  foo2("foo", 123);

  function foo3(a, b, ...c) {
    assert(a === "foo");
    assert(b === 123);
    assert(c instanceof Array);
    assert(c.length === 2);
    assert(c[0] === undefined);
    assert(c[1].foo === "bar");
  }
  foo3("foo", 123, undefined, { foo: "bar" });

  var foo = (...a) => {
    assert(a instanceof Array);
    assert(a.length === 0);
  };
  foo();

  var foo = (a, b, ...c) => {
    assert(a === "foo");
    assert(b === 123);
    assert(c instanceof Array);
    assert(c.length === 2);
    assert(c[0] === undefined);
    assert(c[1].foo === "bar");
  };
  foo("foo", 123, undefined, { foo: "bar" });

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
