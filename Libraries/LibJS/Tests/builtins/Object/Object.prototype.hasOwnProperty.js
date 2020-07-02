load("test-common.js");

try {
    var o = {};
    o.foo = 1;
    assert(o.hasOwnProperty("foo") === true);
    assert(o.hasOwnProperty("bar") === false);
    assert(o.hasOwnProperty() === false);
    assert(o.hasOwnProperty(undefined) === false);
    o.undefined = 2;
    assert(o.hasOwnProperty() === true);
    assert(o.hasOwnProperty(undefined) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
