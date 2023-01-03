test("length is 2", () => {
    expect(Array.prototype.splice).toHaveLength(2);
});

test("basic functionality", () => {
    var array = ["hello", "friends", "serenity", 1, 2];
    var removed = array.splice(3);
    expect(array).toEqual(["hello", "friends", "serenity"]);
    expect(removed).toEqual([1, 2]);

    array = ["hello", "friends", "serenity", 1, 2];
    removed = array.splice(-2);
    expect(array).toEqual(["hello", "friends", "serenity"]);
    expect(removed).toEqual([1, 2]);

    array = ["hello", "friends", "serenity", 1, 2];
    removed = array.splice(-2, 1);
    expect(array).toEqual(["hello", "friends", "serenity", 2]);
    expect(removed).toEqual([1]);

    array = ["serenity"];
    removed = array.splice(0, 0, "hello", "friends");
    expect(array).toEqual(["hello", "friends", "serenity"]);
    expect(removed).toEqual([]);

    array = ["goodbye", "friends", "serenity"];
    removed = array.splice(0, 1, "hello");
    expect(array).toEqual(["hello", "friends", "serenity"]);
    expect(removed).toEqual(["goodbye"]);

    array = ["foo", "bar", "baz"];
    removed = array.splice();
    expect(array).toEqual(["foo", "bar", "baz"]);
    expect(removed).toEqual([]);

    removed = array.splice(0, 123);
    expect(array).toEqual([]);
    expect(removed).toEqual(["foo", "bar", "baz"]);

    array = ["foo", "bar", "baz"];
    removed = array.splice(123, 123);
    expect(array).toEqual(["foo", "bar", "baz"]);
    expect(removed).toEqual([]);

    array = ["foo", "bar", "baz"];
    removed = array.splice(-123, 123);
    expect(array).toEqual([]);
    expect(removed).toEqual(["foo", "bar", "baz"]);
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
        Array.prototype.splice.call(obj, 0);
    }).toThrowWithMessage(RangeError, "Invalid array length");
});
