load("test-common.js");

try {
    assert(String.prototype.substring.length === 2);
    assert("hello friends".substring() === "hello friends");
    assert("hello friends".substring(1) === "ello friends");
    assert("hello friends".substring(0, 5) === "hello");
    assert("hello friends".substring(13, 6) === "friends");
    assert("hello friends".substring('', 5) === "hello");
    assert("hello friends".substring(3, 3) === "");
    assert("hello friends".substring(-1, 13) === "hello friends");
    assert("hello friends".substring(0, 50) === "hello friends");
    assert("hello friends".substring(0, "5") === "hello");
    assert("hello friends".substring("6", "13") === "friends");

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
