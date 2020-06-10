load("test-common.js");

try {
    var bigint = 123n;

    assert(typeof bigint === "bigint");
    assert(-bigint === -123n);
    assert("" + bigint === "123")

    assertThrowsError(() => {
        +bigint;
    }, {
        error: TypeError,
        message: "Cannot convert BigInt to number"
    });

    assert(12n + 34n === 46n);
    assert(12n - 34n === -22n);
    assert(8n * 12n === 96n);
    assert(123n / 10n === 12n);
    assert(2n ** 3n === 8n);
    assert(5n % 3n === 2n);
    assert(45977665298704210987n + 714320987142450987412098743217984576n / 4598741987421098765327980n * 987498743n === 199365500239020623962n);

    assert((12n & 5n) === 4n);
    assert((1n | 2n) === 3n);
    assert((5n ^ 3n) === 6n);
    assert(~1n === -2n);

    bigint = 1n;
    assert(bigint++ === 1n);
    assert(bigint === 2n);
    assert(bigint-- === 2n);
    assert(bigint === 1n);
    assert(++bigint === 2n);
    assert(bigint === 2n);
    assert(--bigint === 1n);
    assert(bigint === 1n);


    assert((1n == 1n) === true);
    assert((1n == 1) === true);
    assert((1 == 1n) === true);
    assert((1n == 1.23) === false);
    assert((1.23 == 1n) === false);

    assert((1n != 1n) === false);
    assert((1n != 1) === false);
    assert((1 != 1n) === false);
    assert((1n != 1.23) === true);
    assert((1.23 != 1n) === true);

    assert((1n === 1n) === true);
    assert((1n === 1) == false);
    assert((1 === 1n) === false);
    assert((1n === 1.23) === false);
    assert((1.23 === 1n) === false);

    assert((1n !== 1n) === false);
    assert((1n !== 1) === true);
    assert((1 !== 1n) === true);
    assert((1n !== 1.23) === true);
    assert((1.23 !== 1n) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
