load("test-common.js");

try {
    [1, null, undefined].forEach(value => {
        assertThrowsError(() => {
            1n + value;
        }, {
            error: TypeError,
            message: "Can't use addition operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n - value;
        }, {
            error: TypeError,
            message: "Can't use subtraction operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n * value;
        }, {
            error: TypeError,
            message: "Can't use multiplication operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n / value;
        }, {
            error: TypeError,
            message: "Can't use division operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n % value;
        }, {
            error: TypeError,
            message: "Can't use modulo operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n ** value;
        }, {
            error: TypeError,
            message: "Can't use exponentiation operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n | value;
        }, {
            error: TypeError,
            message: "Can't use bitwise OR operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n & value;
        }, {
            error: TypeError,
            message: "Can't use bitwise AND operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n ^ value;
        }, {
            error: TypeError,
            message: "Can't use bitwise XOR operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n << value;
        }, {
            error: TypeError,
            message: "Can't use left-shift operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n >> value;
        }, {
            error: TypeError,
            message: "Can't use right-shift operator with BigInt and other type"
        });
        assertThrowsError(() => {
            1n >>> value;
        }, {
            error: TypeError,
            message: "Can't use unsigned right-shift operator with BigInt"
        });
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
