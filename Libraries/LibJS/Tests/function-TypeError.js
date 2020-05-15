load("test-common.js");

try {
    assertThrowsError(() => {
        var b = true;
        b();
    }, {
        error: TypeError,
        message: "true is not a function (evaluated from 'b')"
    });

    assertThrowsError(() => {
        var n = 100 + 20 + 3;
        n();
    }, {
        error: TypeError,
        message: "123 is not a function (evaluated from 'n')"
    });

    assertThrowsError(() => {
        var o = {};
        o.a();
    }, {
        error: TypeError,
        message: "undefined is not a function (evaluated from 'o.a')"
    });

    assertThrowsError(() => {
        Math();
    }, {
        error: TypeError,
        message: "[object MathObject] is not a function (evaluated from 'Math')"
    });

    assertThrowsError(() => {
        new Math();
    }, {
        error: TypeError,
        message: "[object MathObject] is not a constructor (evaluated from 'Math')"
    });

    assertThrowsError(() => {
        new isNaN();
    }, {
        error: TypeError,
        message: "[object NativeFunction] is not a constructor (evaluated from 'isNaN')"
    });

    console.log("PASS");
} catch(e) {
    console.log("FAIL: " + e);
}
