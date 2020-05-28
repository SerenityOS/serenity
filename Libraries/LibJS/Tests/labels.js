load("test-common.js");

try {
    test:
    {
        let o = 1;
        assert(o === 1);
    }

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
