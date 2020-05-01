load("test-common.js");

try {
    var s = "foo";
    assert(s[0] === "f");
    assert(s[1] === "o");
    assert(s[2] === "o");
    assert(s[3] === undefined);

    var o = new String("bar");
    assert(o[0] === "b");
    assert(o[1] === "a");
    assert(o[2] === "r");
    assert(o[3] === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
