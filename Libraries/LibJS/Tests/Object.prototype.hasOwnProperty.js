load("test-common.js");

try {
    var o = {};
    o.foo = 1;
    assert(o.hasOwnProperty("foo") === true);
    assert(o.hasOwnProperty("bar") === false);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
