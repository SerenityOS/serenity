load("test-common.js");

try {
    assert(Array.prototype.findIndex.length === 1);

    assertThrowsError(() => {
        [].findIndex(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var array = ["hello", "friends", 1, 2, false];

    assert(array.findIndex(value => value === "hello") === 0);
    assert(array.findIndex((value, index, arr) => index === 1) === 1);
    assert(array.findIndex(value => value == "1") === 2);
    assert(array.findIndex(value => value === 1) === 2);
    assert(array.findIndex(value => typeof(value) !== "string") === 2);
    assert(array.findIndex(value => typeof(value) === "boolean") === 4);
    assert(array.findIndex(value => value > 1) === 3);
    assert(array.findIndex(value => value > 1 && value < 3) === 3);
    assert(array.findIndex(value => value > 100) === -1);
    assert([].findIndex(value => value === 1) === -1);

    var callbackCalled = 0;
    var callback = () => { callbackCalled++; };

    [].findIndex(callback)
    assert(callbackCalled === 0);

    [1, 2, 3].findIndex(callback);
    assert(callbackCalled === 3);

    callbackCalled = 0;
    [1, , , "foo", , undefined, , ,].findIndex(callback);
    assert(callbackCalled === 8);

    callbackCalled = 0;
    [1, , , "foo", , undefined, , ,].findIndex(value => {
        callbackCalled++;
        return value === undefined;
    });
    assert(callbackCalled === 2);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
