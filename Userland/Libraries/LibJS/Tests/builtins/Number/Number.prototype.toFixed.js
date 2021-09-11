describe("correct behavior", () => {
    test("length", () => {
        expect(Number.prototype.toFixed).toHaveLength(1);
    });

    test("basic functionality", () => {
        [
            [0, 5, "0.00000"],
            [Infinity, 6, "Infinity"],
            [-Infinity, 7, "-Infinity"],
            [NaN, 8, "NaN"],
            [12.81646112, 3, "12.816"],
            [84.23, 4, "84.2300"],
            [3.00003, 5, "3.00003"],
            // Numbers >= 1e+21
            [1e21, 5, "1e+21"],
            [1e22, 0, "1e+22"],
        ].forEach(testCase => {
            expect(testCase[0].toFixed(testCase[1])).toBe(testCase[2]);
        });
    });

    test("decimal fixed digits gets converted to int", () => {
        expect((30.521).toFixed(1.9)).toBe("30.5");
        expect((30.521).toFixed(2.2)).toBe("30.52");
    });
});

describe("errors", () => {
    test("must be called with numeric |this|", () => {
        [true, [], {}, Symbol("foo"), "bar", 1n].forEach(value => {
            expect(() => Number.prototype.toFixed.call(value)).toThrowWithMessage(
                TypeError,
                "Not an object of type Number"
            );
        });
    });

    test("fixed digits RangeError", () => {
        [-Infinity, -5, 105, Infinity, NaN].forEach(value => {
            expect(() => (0).toFixed(value)).toThrow(RangeError);
        });
    });
});
