load("test-common.js");

try {
    var s = "foobar"
    assert(typeof s === "string");
    assert(s.length === 6);

    assert(s.charAt(0) === 'f');
    assert(s.charAt(1) === 'o');
    assert(s.charAt(2) === 'o');
    assert(s.charAt(3) === 'b');
    assert(s.charAt(4) === 'a');
    assert(s.charAt(5) === 'r');
    assert(s.charAt(6) === '');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
