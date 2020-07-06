test("basic functionality", () => {
    var o = new Object();
    Object.prototype.foo = 123;
    expect(o.foo).toBe(123);
});
