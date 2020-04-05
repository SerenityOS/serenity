try {
    try {
        var b = true;
        b();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "true is not a function");
    }

    try {
        var n = 100 + 20 + 3;
        n();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "123 is not a function");
    }

    try {
        var o = {};
        o.a();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "undefined is not a function");
    }

    try {
        Math();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a function");
    }

    try {
        new Math();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "[object MathObject] is not a constructor");
    }

    try {
        new isNaN();
    } catch(e) {
        assert(e.name === "TypeError");
        assert(e.message === "function () {\n  [NativeFunction]\n} is not a constructor");
    }

    console.log("PASS");
} catch(e) {
    console.log("FAIL: " + e);
}
