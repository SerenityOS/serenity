test("basic functionality", () => {
    expect(RegExp().toString()).toBe("/(?:)/");
    expect(RegExp(undefined).toString()).toBe("/(?:)/");
    expect(RegExp("foo").toString()).toBe("/foo/");
    expect(RegExp("foo", undefined).toString()).toBe("/foo/");
    expect(RegExp("foo", "g").toString()).toBe("/foo/g");
    expect(RegExp(undefined, "g").toString()).toBe("/(?:)/g");
});
