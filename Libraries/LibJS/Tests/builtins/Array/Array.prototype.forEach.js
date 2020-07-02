load("test-common.js");

try {
    assert(Array.prototype.forEach.length === 1);

    assertThrowsError(() => {
        [].forEach();
    }, {
        error: TypeError,
        message: "Array.prototype.forEach() requires at least one argument"
    });

    assertThrowsError(() => {
        [].forEach(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var a = [1, 2, 3];
    var o = {};
    var callbackCalled = 0;
    var callback = () => { callbackCalled++; };

    assert([].forEach(callback) === undefined);
    assert(callbackCalled === 0);

    assert(a.forEach(callback) === undefined);
    assert(callbackCalled === 3);

    callbackCalled = 0;
    a.forEach(function(value, index) {
        assert(value === a[index]);
        assert(index === a[index] - 1);
    });

    callbackCalled = 0;
    a.forEach(function(_, _, array) {
        callbackCalled++;
        assert(a.length === array.length);
        a.push("test");
    });
    assert(callbackCalled === 3);
    assert(a.length === 6);

    callbackCalled = 0;
    a.forEach(function(value, index) {
        callbackCalled++;
        this[index] = value;
    }, o);
    assert(callbackCalled === 6);
    assert(o[0] === 1);
    assert(o[1] === 2);
    assert(o[2] === 3);
    assert(o[3] === "test");
    assert(o[4] === "test");
    assert(o[5] === "test");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
