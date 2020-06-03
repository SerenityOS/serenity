load("test-common.js");

try {
    new Proxy({}, {});

    assertThrowsError(() => {
        new Proxy();
    }, {
        error: TypeError,
        message: "Proxy requires at least two arguments",
    });

    assertThrowsError(() => {
        Proxy();
    }, {
        error: TypeError,
        message: "Proxy must be called with the \"new\" operator",
    });

    assertThrowsError(() => {
        new Proxy(1, {});
    }, {
        error: TypeError,
        message: "Expected target argument of Proxy constructor to be object, got 1",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
