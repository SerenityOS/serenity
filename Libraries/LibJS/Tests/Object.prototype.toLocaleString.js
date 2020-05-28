load("test-common.js");

try {
    assert(Object.prototype.toLocaleString.length === 0);

    var o;

    o = {};
    assert(o.toString() === o.toLocaleString());

    o = { toString: () => 42 };
    assert(o.toString() === 42);

    o = { toString: () => { throw Error(); } };
    assertThrowsError(() => {
        o.toLocaleString();
    }, {
        error: Error
    });

    o = { toString: "foo" };
    assertThrowsError(() => {
        o.toLocaleString();
    }, {
        error: TypeError,
        message: "foo is not a function"
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
