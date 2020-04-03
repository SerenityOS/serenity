function assert(x) { if (!x) throw 1; }

try {
    let foo = 1;
    false && (foo = 2);
    assert(foo === 1);

    foo = 1;
    true || (foo = 2);
    assert(foo === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
