load("test-common.js");

try {
    assert(Array.prototype.every.length === 1);

    assertThrowsError(() => {
        [].every(undefined);
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    var arrayOne = ["serenity", { test: "serenity"} ];
    var arrayTwo = [true, false, 1, 2, 3, "3"];

    assert(arrayOne.every(value => value === "hello") === false);
    assert(arrayOne.every(value => value === "serenity") === false);
    assert(arrayOne.every((value, index, arr) => index < 2) === true);
    assert(arrayOne.every(value => typeof(value) === "string") === false);
    assert(arrayOne.every(value => arrayOne.pop()) === true);

    assert(arrayTwo.every((value, index, arr) => index > 0) === false);
    assert(arrayTwo.every((value, index, arr) => index >= 0) === true);
    assert(arrayTwo.every(value => typeof(value) !== "string") === false);
    assert(arrayTwo.every(value => typeof(value) === "number") === false);
    assert(arrayTwo.every(value => value > 0) === false);
    assert(arrayTwo.every(value => value >= 0 && value < 4) === true);
    assert(arrayTwo.every(value => arrayTwo.pop()) === true);

    assert(["", "hello", "friends", "serenity"].every(value => value.length >= 0) === true);
    assert([].every(value => value === 1) === true);

    arrayTwo = [true, false, 1, 2, 3, "3"];

    // Every only goes up to the original length.
    assert(arrayTwo.every((value, index, arr) => {
        arr.push("serenity");
        return value < 4;
    }) === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
