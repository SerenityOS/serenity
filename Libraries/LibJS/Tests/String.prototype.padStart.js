try {
    assert(String.prototype.padStart.length === 1);

    var s = "foo";
    assert(s.padStart(-1) === "foo");
    assert(s.padStart(0) === "foo");
    assert(s.padStart(3) === "foo");
    assert(s.padStart(5) === "  foo");
    assert(s.padStart(10) === "       foo");
    assert(s.padStart("5") === "  foo");
    assert(s.padStart([[["5"]]]) === "  foo");
    assert(s.padStart(2, "+") === "foo");
    assert(s.padStart(5, "+") === "++foo");
    assert(s.padStart(5, 1) === "11foo");
    assert(s.padStart(10, null) === "nullnulfoo");
    assert(s.padStart(10, "bar") === "barbarbfoo");
    assert(s.padStart(10, "123456789") === "1234567foo");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
