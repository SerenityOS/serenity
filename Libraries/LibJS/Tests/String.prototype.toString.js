try {
    assert(String.prototype.toString.length === 0)
    assert("".toString() === "");
    assert("hello friends".toString() === "hello friends");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
