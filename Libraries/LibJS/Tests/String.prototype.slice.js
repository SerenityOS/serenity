load("test-common.js");

try {
    assert(String.prototype.slice.length === 2);
    assert("hello friends".slice() === "hello friends");
    assert("hello friends".slice(1) === "ello friends");
    assert("hello friends".slice(0, 5) === "hello");
    assert("hello friends".slice(13, 6) === "");
    assert("hello friends".slice('', 5) === "hello");
    assert("hello friends".slice(3, 3) === "");
    assert("hello friends".slice(-1, 13) === "s");
    assert("hello friends".slice(0, 50) === "hello friends");
    assert("hello friends".slice(0, "5") === "hello");
    assert("hello friends".slice("6", "13") === "friends");
    assert("hello friends".slice(-7) === "friends");
    assert("hello friends".slice(1000) === "");
    assert("hello friends".slice(-1000) === "hello friends");

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
