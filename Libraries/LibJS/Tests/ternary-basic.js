try {
    var x = 1;

    assert(x === 1 ? true : false);
    assert((x ? x : 0) === x);
    assert(1 < 2 ? (true) : (false));

    var o = {};
    o.f = true;
    assert(o.f ? true : false);

    assert(1 ? o.f : null);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
