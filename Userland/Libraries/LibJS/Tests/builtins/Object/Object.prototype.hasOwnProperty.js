test("basic functionality", () => {
    var o = {};

    o.foo = 1;
    expect(o.hasOwnProperty("foo")).toBeTrue();
    expect(o.hasOwnProperty("bar")).toBeFalse();
    expect(o.hasOwnProperty()).toBeFalse();
    expect(o.hasOwnProperty(undefined)).toBeFalse();

    o.undefined = 2;
    expect(o.hasOwnProperty()).toBeTrue();
    expect(o.hasOwnProperty(undefined)).toBeTrue();
});
