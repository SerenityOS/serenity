describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.PlainDateTime.prototype.toString).toHaveLength(0);
    });

    test("basic functionality", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 11, 3, 1, 33, 5, 100, 200, 300);
        expect(plainDateTime.toString()).toBe("2021-11-03T01:33:05.1002003");
    });

    test("fractionalSecondDigits option", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 11, 3, 1, 33, 5, 100, 200, 300);
        const values = [
            ["auto", "2021-11-03T01:33:05.1002003"],
            [0, "2021-11-03T01:33:05"],
            [1, "2021-11-03T01:33:05.1"],
            [2, "2021-11-03T01:33:05.10"],
            [3, "2021-11-03T01:33:05.100"],
            [4, "2021-11-03T01:33:05.1002"],
            [5, "2021-11-03T01:33:05.10020"],
            [6, "2021-11-03T01:33:05.100200"],
            [7, "2021-11-03T01:33:05.1002003"],
            [8, "2021-11-03T01:33:05.10020030"],
            [9, "2021-11-03T01:33:05.100200300"],
        ];

        for (const [fractionalSecondDigits, expected] of values) {
            const options = { fractionalSecondDigits };
            expect(plainDateTime.toString(options)).toBe(expected);
        }

        // Ignored when smallestUnit is given
        expect(plainDateTime.toString({ smallestUnit: "minute", fractionalSecondDigits: 9 })).toBe(
            "2021-11-03T01:33"
        );
    });

    test("smallestUnit option", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 11, 3, 1, 33, 5, 100, 200, 300);
        const values = [
            ["minute", "2021-11-03T01:33"],
            ["second", "2021-11-03T01:33:05"],
            ["millisecond", "2021-11-03T01:33:05.100"],
            ["microsecond", "2021-11-03T01:33:05.100200"],
            ["nanosecond", "2021-11-03T01:33:05.100200300"],
        ];

        for (const [smallestUnit, expected] of values) {
            const singularOptions = { smallestUnit };
            const pluralOptions = { smallestUnit: `${smallestUnit}s` };
            expect(plainDateTime.toString(singularOptions)).toBe(expected);
            expect(plainDateTime.toString(pluralOptions)).toBe(expected);
        }
    });
});

describe("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Temporal.PlainDateTime.prototype.toString.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.PlainDateTime");
    });
});
