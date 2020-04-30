load("test-common.js");

try {
    let local = Symbol('foo');
    let global = Symbol.for('foo');
    assert(local.valueOf() === local);
    assert(global.valueOf() === global);

    assert(Symbol.prototype.valueOf.call(local) === local);
    assert(Symbol.prototype.valueOf.call(global) === global);

    assertThrowsError(() => {
        Symbol.prototype.valueOf.call("foo");
    }, {
        error: TypeError,
        message: "object must be of type Symbol"
    });

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
