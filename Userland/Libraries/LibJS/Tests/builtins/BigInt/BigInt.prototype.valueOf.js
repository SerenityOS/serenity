test("basic functionality", () => {
    expect(BigInt.prototype.valueOf).toHaveLength(0);
    expect(typeof BigInt(123).valueOf()).toBe("bigint");
    // FIXME: Uncomment once we support Object() with argument
    // expect(typeof Object(123n).valueOf()).toBe("bigint");
});

test("calling with non-BigInt |this|", () => {
    expect(() => {
        BigInt.prototype.valueOf.call("foo");
    }).toThrowWithMessage(TypeError, "Not a BigInt object");
});
