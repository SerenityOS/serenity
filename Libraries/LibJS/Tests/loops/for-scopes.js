test("var in for head", () => {
    for (var v = 5; false; );
    expect(v).toBe(5);
});

test("let in for head", () => {
    for (let l = 5; false; );
    expect(() => {
        l;
    }).toThrowWithMessage(ReferenceError, "'l' is not defined");
});

test("const in for head", () => {
    for (const c = 5; false; );
    expect(() => {
        c;
    }).toThrowWithMessage(ReferenceError, "'c' is not defined");
});
