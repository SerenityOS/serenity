load("test-common.js");

try {
    var o = {};
    o.f = 1;

    assert(o.f++ === 1);
    assert(++o.f === 3);
    assert(isNaN(++o.missing));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
