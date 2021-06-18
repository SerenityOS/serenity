test("indexing the array doesn't strip whitespace if it's a number", () => {
    var a = [];
    a[1] = 1;

    expect(a["1"]).toBe(1);
    expect(a[" 1 "]).toBeUndefined();
});
