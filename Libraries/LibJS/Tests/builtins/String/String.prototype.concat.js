load("test-common.js");

try {
    assert(String.prototype.concat.length === 1);
    assert("".concat(1) === "1");
    assert("".concat(3,2,1) === "321");
    assert("hello".concat(" ", "friends") === "hello friends");
    assert("".concat(null) === "null");
    assert("".concat(false) === "false");
    assert("".concat(true) === "true");
    assert("".concat([]) === "");
    assert("".concat([1, 2, 3, 'hello']) === "1,2,3,hello");
    assert("".concat(true, []) === "true");
    assert("".concat(true, false) === "truefalse");
    assert("".concat({}) === "[object Object]");
    assert("".concat(1, {}) === "1[object Object]");
    assert("".concat(1, {}, false) === "1[object Object]false");

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
