load("test-common.js");

try {
    assert(Array.prototype.find.length === 1);

    assertThrowsError(() => {
        [].find(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var array = ["hello", "friends", 1, 2, false];

    assert(array.find(value => value === "hello") === "hello");
    assert(array.find((value, index, arr) => index === 1) === "friends");
    assert(array.find(value => value == "1") === 1);
    assert(array.find(value => value === 1) === 1);
    assert(array.find(value => typeof(value) !== "string") === 1);
    assert(array.find(value => typeof(value) === "boolean") === false);
    assert(array.find(value => value > 1) === 2);
    assert(array.find(value => value > 1 && value < 3) === 2);
    assert(array.find(value => value > 100) === undefined);
    assert([].find(value => value === 1) === undefined);

    var callbackCalled = 0;
    var callback = () => { callbackCalled++; };

    [].find(callback)
    assert(callbackCalled === 0);

    [1, 2, 3].find(callback);
    assert(callbackCalled === 3);

    callbackCalled = 0;
    [1, , , "foo", , undefined, , ,].find(callback);
    assert(callbackCalled === 8);

    callbackCalled = 0;
    [1, , , "foo", , undefined, , ,].find(value => {
        callbackCalled++;
        return value === undefined;
    });
    assert(callbackCalled === 2);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
