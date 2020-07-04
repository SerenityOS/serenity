test("length is 0", () => {
    expect(Array.prototype.shift).toHaveLength(0);
});

describe("normal behavior", () => {
    test("array with elements", () => {
        var a = [1, 2, 3];
        expect(a.shift()).toBe(1);
        expect(a).toEqual([2, 3]);
    });

    test("empty array", () => {
        var a = [];
        expect(a.shift()).toBeUndefined();
        expect(a).toEqual([]);
    });

    test("array with empty slot", () => {
        var a = [,];
        expect(a.shift()).toBeUndefined();
        expect(a).toEqual([]);
    });
});
