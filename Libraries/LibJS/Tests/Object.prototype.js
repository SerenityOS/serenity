try {
    var o = new Object();
    Object.prototype.foo = 123;
    assert(o.foo === 123);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
