try {
    assert(Array.length === 1);
    assert(Array.prototype.length === 0);
    assert(typeof Array() === "object");
    assert(typeof new Array() === "object");

    x = new Array(5);

    assert(x.length === 5);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
