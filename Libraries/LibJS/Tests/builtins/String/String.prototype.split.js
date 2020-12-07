test("string split", () => {
    expect("foo bar baz".split(" ")).toEqual(["foo", "bar", "baz"]);
    expect("foo,bar,baz".split(",")).toEqual(["foo", "bar", "baz"]);
    expect("foo||bar||baz".split("||")).toEqual(["foo", "bar", "baz"]);
});
