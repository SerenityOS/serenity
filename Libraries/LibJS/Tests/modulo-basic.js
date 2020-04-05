function assert(x) { if (!x) throw 1; }

try {
    assert(10 % 3 === 1);
    assert(10.5 % 2.5 === 0.5);
    assert(-0.99 % 0.99 === -0);

    // Examples form MDN:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Arithmetic_Operators
    assert(12 % 5 === 2);
    assert(-1 % 2 === -1);
    assert(1 % -2 === 1);
    assert(isNaN(NaN % 2));
    assert(1 % 2 === 1);
    assert(2 % 3 === 2);
    assert(-4 % 2 === -0);
    assert(5.5 % 2 === 1.5);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
