describe("correct behavior", () => {
    test("length", () => {
        expect(Number.prototype.toString).toHaveLength(1);
    });

    test("basic functionality", () => {
        [
            [+0, "0"],
            [-0, "0"],
            [Infinity, "Infinity"],
            [-Infinity, "-Infinity"],
            [NaN, "NaN"],
            [12, "12"],
            [93465, "93465"],
            [358000, "358000"],
        ].forEach(testCase => {
            expect(testCase[0].toString()).toBe(testCase[1]);
        });
    });

    test("radix", () => {
        let number = 7857632;

        [
            [2, "11101111110010111100000"],
            [3, "112210012122102"],
            [4, "131332113200"],
            [5, "4002421012"],
            [6, "440225532"],
            [7, "123534356"],
            [8, "35762740"],
            [9, "15705572"],
            [10, "7857632"],
            [11, "4487612"],
            [12, "276b2a8"],
            [13, "18216b3"],
            [14, "10877d6"],
            [15, "a532c2"],
            [16, "77e5e0"],
            [17, "59160b"],
            [18, "42f5h2"],
            [19, "335b5b"],
            [20, "29241c"],
            [21, "1j89fk"],
            [22, "1bbkh2"],
            [23, "151ih4"],
            [24, "ng9h8"],
            [25, "k2m57"],
            [26, "h51ig"],
            [27, "el5hb"],
            [28, "clqdk"],
            [29, "b355o"],
            [30, "9l0l2"],
            [31, "8fng0"],
            [32, "7fpf0"],
            [33, "6klf2"],
            [34, "5tv8s"],
            [35, "589dr"],
            [36, "4oezk"],
        ].forEach(testCase => {
            expect(number.toString(testCase[0])).toBe(testCase[1]);
        });
    });

    test("decimal radix gets converted to int", () => {
        expect((30).toString(10.1)).toBe("30");
        expect((30).toString(10.9)).toBe("30");
    });
});

test("errors", () => {
    test("must be called with numeric |this|", () => {
        [true, [], {}, Symbol("foo"), "bar", 1n].forEach(value => {
            expect(() => Number.prototype.toString.call(value)).toThrow(TypeError);
        });
    });

    test("radix RangeError", () => {
        [0, 1, 37, 100].forEach(value => {
            expect(() => (0).toString(value)).toThrow(RangeError);
        });
    });
});
