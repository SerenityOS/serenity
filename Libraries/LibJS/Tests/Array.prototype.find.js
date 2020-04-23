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

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
