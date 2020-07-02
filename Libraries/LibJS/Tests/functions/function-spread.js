load("test-common.js");

try {
    const sum = (a, b, c) => a + b + c;
    const a = [1, 2, 3];

    assert(sum(...a) === 6);
    assert(sum(1, ...a) === 4);
    assert(sum(...a, 10) === 6);

    const foo = (a, b, c) => c;

    const o = { bar: [1, 2, 3] };
    assert(foo(...o.bar) === 3);
    assert(foo(..."abc") === "c");

    assertThrowsError(() => {
        [...1];
    }, {
        error: TypeError,
        message: "1 is not iterable",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
