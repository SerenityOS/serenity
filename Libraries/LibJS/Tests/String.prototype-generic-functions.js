load("test-common.js");

try {
    const genericStringPrototypeFunctions = [
        "charAt",
        "repeat",
        "startsWith",
        "indexOf",
        "toLowerCase",
        "toUpperCase",
        "padStart",
        "padEnd",
        "trim",
        "trimStart",
        "trimEnd",
        "concat",
        "substring",
        "includes",
    ];

    genericStringPrototypeFunctions.forEach(name => {
        String.prototype[name].call({ toString: () => "hello friends" });
        String.prototype[name].call({ toString: () => 123 });
        String.prototype[name].call({ toString: () => undefined });

        assertThrowsError(() => {
            String.prototype[name].call({ toString: () => new String() });
        }, {
            error: TypeError,
            message: "Cannot convert object to string"
        });

        assertThrowsError(() => {
            String.prototype[name].call({ toString: () => [] });
        }, {
            error: TypeError,
            message: "Cannot convert object to string"
        });

        assertThrowsError(() => {
            String.prototype[name].call({ toString: () => ({}) });
        }, {
            error: TypeError,
            message: "Cannot convert object to string"
        });
    });

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
