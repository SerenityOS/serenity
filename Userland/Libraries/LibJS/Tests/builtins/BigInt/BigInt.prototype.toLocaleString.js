test("basic functionality", () => {
    expect(BigInt.prototype.toLocaleString).toHaveLength(0);
    expect(BigInt(123).toLocaleString()).toBe("123");
});

test("calling with non-BigInt |this|", () => {
    expect(() => {
        BigInt.prototype.toLocaleString.call("foo");
    }).toThrowWithMessage(TypeError, "Not a BigInt object");
});
