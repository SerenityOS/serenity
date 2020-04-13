load("test-common.js");

try {
    try {
        var b = true;
        b();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "true is not a function");
    }

    try {
        var n = 100 + 20 + 3;
        n();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "123 is not a function");
    }

    try {
        var o = {};
        o.a();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "undefined is not a function");
    }

    try {
        Math();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a function");
    }

    try {
        new Math();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a constructor");
    }

    try {
        new isNaN();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "function isNaN() {\n  [NativeFunction]\n} is not a constructor");
    }

    console.log("PASS");
} catch(e) {
    console.log("FAIL: " + e);
}
