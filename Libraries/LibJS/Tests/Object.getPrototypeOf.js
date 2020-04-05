try {
    var o1 = new Object();
    var o2 = {};
    assert(Object.getPrototypeOf(o1) === Object.getPrototypeOf(o2));
    assert(Object.getPrototypeOf(Object.getPrototypeOf(o1)) === null);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
