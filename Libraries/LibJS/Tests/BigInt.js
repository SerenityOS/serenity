load("test-common.js");

try {
    assert(BigInt.length === 1);
    assert(BigInt.name === "BigInt");

    assert(BigInt(0) === 0n);
    assert(BigInt(1) === 1n);
    assert(BigInt(+1) === 1n);
    assert(BigInt(-1) === -1n);
    assert(BigInt("") === 0n);
    assert(BigInt("0") === 0n);
    assert(BigInt("1") === 1n);
    assert(BigInt("+1") === 1n);
    assert(BigInt("-1") === -1n);
    assert(BigInt("-1") === -1n);
    assert(BigInt([]) === 0n);
    assert(BigInt("42") === 42n);
    assert(BigInt("  \n  00100  \n  ") === 100n);
    assert(BigInt(123n) === 123n);
    assert(BigInt("3323214327642987348732109829832143298746432437532197321") === 3323214327642987348732109829832143298746432437532197321n);

    assertThrowsError(() => {
        new BigInt();
    }, {
        error: TypeError,
        message: "BigInt is not a constructor"
    });

    [null, undefined, Symbol()].forEach(value => {
        assertThrowsError(() => {
            BigInt(value);
        }, {
            error: TypeError,
            message: typeof value === "symbol"
                ? "Cannot convert symbol to BigInt"
                : `Cannot convert ${value} to BigInt`
        });
    });

    ["foo", "123n", "1+1", {}, function () { }].forEach(value => {
        assertThrowsError(() => {
            BigInt(value);
        }, {
            error: SyntaxError,
            message: `Invalid value for BigInt: ${value}`
        });
    });

    [1.23, Infinity, -Infinity, NaN].forEach(value => {
        assertThrowsError(() => {
            BigInt(value);
        }, {
            error: RangeError,
            message: "BigInt argument must be an integer"
        });
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
