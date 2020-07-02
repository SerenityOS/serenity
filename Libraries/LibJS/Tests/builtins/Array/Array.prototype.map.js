load("test-common.js");

try {
    assert(Array.prototype.map.length === 1);

    assertThrowsError(() => {
        [].map();
    }, {
        error: TypeError,
        message: "Array.prototype.map() requires at least one argument"
    });

    assertThrowsError(() => {
        [].map(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var callbackCalled = 0;
    var callback = () => { callbackCalled++; };

    assert([].map(callback).length === 0);
    assert(callbackCalled === 0);

    assert([1, 2, 3].map(callback).length === 3);
    assert(callbackCalled === 3);

    callbackCalled = 0;
    assert([1, , , "foo", , undefined, , ,].map(callback).length === 8);
    assert(callbackCalled === 3);

    var results = [undefined, null, true, "foo", 42, {}].map((value, index) => "" + index + " -> " + value);
    assert(results.length === 6);
    assert(results[0] === "0 -> undefined");
    assert(results[1] === "1 -> null");
    assert(results[2] === "2 -> true");
    assert(results[3] === "3 -> foo");
    assert(results[4] === "4 -> 42");
    assert(results[5] === "5 -> [object Object]");

    var squaredNumbers = [0, 1, 2, 3, 4].map(x => x ** 2);
    assert(squaredNumbers.length === 5);
    assert(squaredNumbers[0] === 0);
    assert(squaredNumbers[1] === 1);
    assert(squaredNumbers[2] === 4);
    assert(squaredNumbers[3] === 9);
    assert(squaredNumbers[4] === 16);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
