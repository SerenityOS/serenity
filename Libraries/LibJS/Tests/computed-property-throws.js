test("Issue #3459, exception in computed property expression", () => {
    expect(() => {
        "foo"[bar];
    }).toThrow(ReferenceError);
    expect(() => {
        "foo"[bar]();
    }).toThrow(ReferenceError);
});
