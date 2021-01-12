describe("errors", () => {
    test("invalid array length value", () => {
        var a = [1, 2, 3];
        [undefined, "foo", -1, Infinity, -Infinity, NaN].forEach(value => {
            expect(() => {
                a.length = value;
            }).toThrowWithMessage(RangeError, "Invalid array length");
            expect(a).toHaveLength(3);
        });
    });
});

describe("normal behavior", () => {
    test("extend array by setting length", () => {
        var a = [1, 2, 3];
        a.length = 5;
        expect(a).toEqual([1, 2, 3, undefined, undefined]);
    });

    test("truncate array by setting length", () => {
        var a = [1, 2, 3];
        a.length = 2;
        expect(a).toEqual([1, 2]);
        a.length = 0;
        expect(a).toEqual([]);
    });

    test("length value is coerced to number if possible", () => {
        var a = [1, 2, 3];
        a.length = "42";
        expect(a).toHaveLength(42);
        a.length = [];
        expect(a).toHaveLength(0);
        a.length = true;
        expect(a).toHaveLength(1);
    });
});
