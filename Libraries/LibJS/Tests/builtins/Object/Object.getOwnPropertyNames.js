test("use with array", () => {
    let names = Object.getOwnPropertyNames([1, 2, 3]);
    expect(names).toEqual(["0", "1", "2", "length"]);
});

test("use with object", () => {
    let names = Object.getOwnPropertyNames({ foo: 1, bar: 2, baz: 3 });
    expect(names).toEqual(["foo", "bar", "baz"]);
});
