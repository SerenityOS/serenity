test("iterate through empty string", () => {
    const a = [];
    for (const property in "") {
        a.push(property);
    }
    expect(a).toEqual([]);
});

test("iterate through number", () => {
    const a = [];
    for (const property in 123) {
        a.push(property);
    }
    expect(a).toEqual([]);
});

test("iterate through empty object", () => {
    const a = [];
    for (const property in {}) {
        a.push(property);
    }
    expect(a).toEqual([]);
});

test("iterate through string", () => {
    const a = [];
    for (const property in "hello") {
        a.push(property);
    }
    expect(a).toEqual(["0", "1", "2", "3", "4"]);
});

test("iterate through object", () => {
    const a = [];
    for (const property in { a: 1, b: 2, c: 2 }) {
        a.push(property);
    }
    expect(a).toEqual(["a", "b", "c"]);
});

test("use already-declared variable", () => {
    var property;
    for (property in "abc");
    expect(property).toBe("2");
});
