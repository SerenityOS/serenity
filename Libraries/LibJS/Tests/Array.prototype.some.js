load("test-common.js");

try {
    assert(Array.prototype.some.length === 1);

    assertThrowsError(() => {
        [].some(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var array = ["hello", "friends", 1, 2, false, -42, { name: "serenityos"}];

    assert(array.some(value => value === "hello") === true);
    assert(array.some(value => value === "serenity") === false);
    assert(array.some((value, index, arr) => index === 1) === true);
    assert(array.some(value => value == "1") === true);
    assert(array.some(value => value === 1) === true);
    assert(array.some(value => value === 13) === false);
    assert(array.some(value => typeof(value) !== "string") === true);
    assert(array.some(value => typeof(value) === "boolean") === true);
    assert(array.some(value => value > 1) === true);
    assert(array.some(value => value > 1 && value < 3) === true);
    assert(array.some(value => value > 100) === false);
    assert(array.some(value => value < 0) === true);
    assert(array.some(value => array.pop()) === true);
    assert(["", "hello", "friends", "serenity"].some(value => value.length === 0) === true);
    assert([].some(value => value === 1) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
