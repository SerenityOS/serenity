test("length is 0", () => {
    expect(Array.prototype.reverse).toHaveLength(0);
});

test("basic functionality", () => {
    var array = [1, 2, 3];
    expect(array.reverse()).toEqual([3, 2, 1]);
    expect(array).toEqual([3, 2, 1]);
});
