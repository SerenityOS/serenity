test("basic functionality", () => {
    expect(String.prototype.split).toHaveLength(2);

    expect("hello friends".split()).toEqual(["hello friends"]);
    expect("hello friends".split("")).toEqual([
        "h",
        "e",
        "l",
        "l",
        "o",
        " ",
        "f",
        "r",
        "i",
        "e",
        "n",
        "d",
        "s",
    ]);
    expect("hello friends".split(" ")).toEqual(["hello", "friends"]);

    expect("a,b,c,d".split(",")).toEqual(["a", "b", "c", "d"]);
    expect(",a,b,c,d".split(",")).toEqual(["", "a", "b", "c", "d"]);
    expect("a,b,c,d,".split(",")).toEqual(["a", "b", "c", "d", ""]);
    expect("a,b,,c,d".split(",")).toEqual(["a", "b", "", "c", "d"]);
    expect(",a,b,,c,d,".split(",")).toEqual(["", "a", "b", "", "c", "d", ""]);
    expect(",a,b,,,c,d,".split(",,")).toEqual([",a,b", ",c,d,"]);
});

test("limits", () => {
    expect("a b c d".split(" ", 0)).toEqual([]);
    expect("a b c d".split(" ", 1)).toEqual(["a"]);
    expect("a b c d".split(" ", 3)).toEqual(["a", "b", "c"]);
    expect("a b c d".split(" ", 100)).toEqual(["a", "b", "c", "d"]);
});
