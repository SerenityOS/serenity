function assert(x) { if (!x) throw 1; }

try {
    var s = "foobar";
    assert(s.startsWith("f") === true);
    assert(s.startsWith("fo") === true);
    assert(s.startsWith("foo") === true);
    assert(s.startsWith("foob") === true);
    assert(s.startsWith("fooba") === true);
    assert(s.startsWith("foobar") === true);
    assert(s.startsWith("foobar1") === false);
    assert(s.startsWith("f", 0) === true);
    assert(s.startsWith("fo", 0) === true);
    assert(s.startsWith("foo", 0) === true);
    assert(s.startsWith("foob", 0) === true);
    assert(s.startsWith("fooba", 0) === true);
    assert(s.startsWith("foobar", 0) === true);
    assert(s.startsWith("foobar1", 0) === false);
    assert(s.startsWith("foo", []) === true);
    assert(s.startsWith("foo", null) === true);
    assert(s.startsWith("foo", undefined) === true);
    assert(s.startsWith("foo", false) === true);
    assert(s.startsWith("foo", true) === false);
    assert(s.startsWith("foo", "foo") === true);
    assert(s.startsWith("foo", 0 - 1) === true);
    assert(s.startsWith("foo", 42) === false);
    assert(s.startsWith("bar", 3) === true);
    assert(s.startsWith("bar", "3") === true);
    assert(s.startsWith("bar1", 3) === false);
    assert(s.startsWith() === false);
    assert(s.startsWith("") === true);
    assert(s.startsWith("", 0) === true);
    assert(s.startsWith("", 1) === true);
    assert(s.startsWith("", 0 - 1) === true);
    assert(s.startsWith("", 42) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
