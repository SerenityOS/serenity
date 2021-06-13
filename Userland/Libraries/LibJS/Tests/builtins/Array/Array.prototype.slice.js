test("length is 0", () => {
    expect(Array.prototype.slice).toHaveLength(2);
});

test("basic functionality", () => {
    var array = ["hello", "friends", "serenity", 1];

    var slice = array.slice();
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["hello", "friends", "serenity", 1]);

    slice = array.slice(1);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["friends", "serenity", 1]);

    slice = array.slice(0, 2);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["hello", "friends"]);

    slice = array.slice(-1);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual([1]);

    slice = array.slice(1, 1);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual([]);

    slice = array.slice(1, -1);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["friends", "serenity"]);

    slice = array.slice(2, -1);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["serenity"]);

    slice = array.slice(0, 100);
    expect(array).toEqual(["hello", "friends", "serenity", 1]);
    expect(slice).toEqual(["hello", "friends", "serenity", 1]);
});

// FIXME: These tests are currently skipped because an invalid array length in this case is 2**32 or above.
//        The codebase currently uses size_t for lengths, which is currently the same as u32 when building for Serenity.
//        This means these lengths wrap around to 0, making the test not work correctly.
test.skip("Invalid lengths", () => {
    var length = Math.pow(2, 32);

    var obj = {
        length: length,
    };

    expect(() => {
        Array.prototype.slice.call(obj, 0);
    }).toThrowWithMessage(RangeError, "Invalid array length");
});
