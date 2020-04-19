load("test-common.js");

try {
    try {
        var b = true;
        b();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "true is not a function (evaluated from 'b')");
    }

    try {
        var n = 100 + 20 + 3;
        n();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "123 is not a function (evaluated from 'n')");
    }

    try {
        var o = {};
        o.a();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "undefined is not a function (evaluated from 'o.a')");
    }

    try {
        Math();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a function (evaluated from 'Math')");
    }

    try {
        new Math();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a constructor (evaluated from 'Math')");
    }

    try {
        new isNaN();
        assertNotReached();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "function isNaN() {\n  [NativeFunction]\n} is not a constructor (evaluated from 'isNaN')");
    }

    console.log("PASS");
} catch(e) {
    console.log("FAIL: " + e);
}
