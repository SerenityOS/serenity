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

test("iterate through undefined", () => {
    for (const property in undefined) {
        expect.fail();
    }
});

test("use already-declared variable", () => {
    var property;
    for (property in "abc");
    expect(property).toBe("2");
});

test("allow binding patterns", () => {
    const expected = [
        ["1", "3", []],
        ["s", undefined, []],
        ["l", "n", ["g", "N", "a", "m", "e"]],
    ];
    let counter = 0;

    for (let [a, , b, ...c] in { 123: 1, sm: 2, longName: 3 }) {
        expect(a).toBe(expected[counter][0]);
        expect(b).toBe(expected[counter][1]);
        expect(c).toEqual(expected[counter][2]);
        counter++;
    }
    expect(counter).toBe(3);
});

test("allow member expression as variable", () => {
    const f = {};
    for (f.a in "abc");
    expect(f.a).toBe("2");
});
