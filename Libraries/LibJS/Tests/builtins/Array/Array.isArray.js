test("length is 1", () => {
    expect(Array.isArray).toHaveLength(1);
});

test("arguments that evaluate to false", () => {
    expect(Array.isArray()).toBeFalse();
    expect(Array.isArray("1")).toBeFalse();
    expect(Array.isArray("foo")).toBeFalse();
    expect(Array.isArray(1)).toBeFalse();
    expect(Array.isArray(1, 2, 3)).toBeFalse();
    expect(Array.isArray(undefined)).toBeFalse();
    expect(Array.isArray(null)).toBeFalse();
    expect(Array.isArray(Infinity)).toBeFalse();
    expect(Array.isArray({})).toBeFalse();
});

test("arguments that evaluate to true", () => {
    expect(Array.isArray([])).toBeTrue();
    expect(Array.isArray([1])).toBeTrue();
    expect(Array.isArray([1, 2, 3])).toBeTrue();
    expect(Array.isArray(new Array())).toBeTrue();
    expect(Array.isArray(new Array(10))).toBeTrue();
    expect(Array.isArray(new Array("a", "b", "c"))).toBeTrue();
    // FIXME: Array.prototype is supposed to be an array!
    // expect(Array.isArray(Array.prototype)).toBeTrue();
});
