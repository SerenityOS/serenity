load("test-common.js");

try {
    let p = new Proxy([], {
        get(_, key) {
            if (key === "length")
                return 3;
            return Number(key);
        },
    });

    assert(JSON.stringify(p) === "[0,1,2]");
    assert(JSON.stringify([[new Proxy(p, {})]]) === "[[[0,1,2]]]");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
