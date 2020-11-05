test("length is 0", () => {
    expect(Array.prototype.pop).toHaveLength(0);
});

describe("normal behavior", () => {
    test("array with elements", () => {
        var a = [1, 2, 3];
        expect(a.pop()).toBe(3);
        expect(a).toEqual([1, 2]);
        expect(a.pop()).toBe(2);
        expect(a).toEqual([1]);
        expect(a.pop()).toBe(1);
        expect(a).toEqual([]);
        expect(a.pop()).toBeUndefined();
        expect(a).toEqual([]);
    });

    test("empty array", () => {
        var a = [];
        expect(a.pop()).toBeUndefined();
        expect(a).toEqual([]);
    });

    test("array with empty slot", () => {
        var a = [,];
        expect(a.pop()).toBeUndefined();
        expect(a).toEqual([]);
    });
});
