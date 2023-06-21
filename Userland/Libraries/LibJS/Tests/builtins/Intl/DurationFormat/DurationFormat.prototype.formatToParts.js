describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Intl.DurationFormat.prototype.formatToParts).toHaveLength(1);
    });

    test("formats duration correctly", () => {
        const duration = {
            years: 1,
            months: 2,
            weeks: 3,
            days: 3,
            hours: 4,
            minutes: 5,
            seconds: 6,
            milliseconds: 7,
            microseconds: 8,
            nanoseconds: 9,
        };
        expect(new Intl.DurationFormat().formatToParts(duration)).toEqual([
            { type: "element", value: "1 yr" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 mths" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 wks" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 days" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 hr" },
            { type: "literal", value: ", " },
            { type: "element", value: "5 min" },
            { type: "literal", value: ", " },
            { type: "element", value: "6 sec" },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 μs" },
            { type: "literal", value: ", " },
            { type: "element", value: "9 ns" },
        ]);
        expect(new Intl.DurationFormat("en").formatToParts(duration)).toEqual([
            { type: "element", value: "1 yr" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 mths" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 wks" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 days" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 hr" },
            { type: "literal", value: ", " },
            { type: "element", value: "5 min" },
            { type: "literal", value: ", " },
            { type: "element", value: "6 sec" },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 μs" },
            { type: "literal", value: ", " },
            { type: "element", value: "9 ns" },
        ]);
        expect(new Intl.DurationFormat("en", { style: "long" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1 year" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 months" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 weeks" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 days" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 hours" },
            { type: "literal", value: ", " },
            { type: "element", value: "5 minutes" },
            { type: "literal", value: ", " },
            { type: "element", value: "6 seconds" },
            { type: "literal", value: ", " },
            { type: "element", value: "7 milliseconds" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 microseconds" },
            { type: "literal", value: ", " },
            { type: "element", value: "9 nanoseconds" },
        ]);
        expect(new Intl.DurationFormat("en", { style: "short" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1 yr" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 mths" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 wks" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 days" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 hr" },
            { type: "literal", value: ", " },
            { type: "element", value: "5 min" },
            { type: "literal", value: ", " },
            { type: "element", value: "6 sec" },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 μs" },
            { type: "literal", value: ", " },
            { type: "element", value: "9 ns" },
        ]);
        expect(new Intl.DurationFormat("en", { style: "narrow" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1y" },
            { type: "literal", value: " " },
            { type: "element", value: "2m" },
            { type: "literal", value: " " },
            { type: "element", value: "3w" },
            { type: "literal", value: " " },
            { type: "element", value: "3d" },
            { type: "literal", value: " " },
            { type: "element", value: "4h" },
            { type: "literal", value: " " },
            { type: "element", value: "5m" },
            { type: "literal", value: " " },
            { type: "element", value: "6s" },
            { type: "literal", value: " " },
            { type: "element", value: "7ms" },
            { type: "literal", value: " " },
            { type: "element", value: "8μs" },
            { type: "literal", value: " " },
            { type: "element", value: "9ns" },
        ]);
        expect(new Intl.DurationFormat("en", { style: "digital" }).formatToParts(duration)).toEqual(
            [
                { type: "element", value: "1 yr" },
                { type: "literal", value: ", " },
                { type: "element", value: "2 mths" },
                { type: "literal", value: ", " },
                { type: "element", value: "3 wks" },
                { type: "literal", value: ", " },
                { type: "element", value: "3 days" },
                { type: "literal", value: ", " },
                { type: "element", value: "4:05:06" },
            ]
        );
        expect(
            new Intl.DurationFormat("en", {
                style: "narrow",
                nanoseconds: "numeric",
                fractionalDigits: 3,
            }).formatToParts(duration)
        ).toEqual([
            { type: "element", value: "1y" },
            { type: "literal", value: " " },
            { type: "element", value: "2m" },
            { type: "literal", value: " " },
            { type: "element", value: "3w" },
            { type: "literal", value: " " },
            { type: "element", value: "3d" },
            { type: "literal", value: " " },
            { type: "element", value: "4h" },
            { type: "literal", value: " " },
            { type: "element", value: "5m" },
            { type: "literal", value: " " },
            { type: "element", value: "6s" },
            { type: "literal", value: " " },
            { type: "element", value: "7ms" },
            { type: "literal", value: " " },
            { type: "element", value: "8.009μs" },
        ]);

        expect(new Intl.DurationFormat("de", { style: "long" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1 Jahr" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 Monate" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 Wochen" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 Tage" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 Stunden" },
            { type: "literal", value: ", " },
            { type: "element", value: "5 Minuten" },
            { type: "literal", value: ", " },
            { type: "element", value: "6 Sekunden" },
            { type: "literal", value: ", " },
            { type: "element", value: "7 Millisekunden" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 Mikrosekunden" },
            { type: "literal", value: " und " },
            { type: "element", value: "9 Nanosekunden" },
        ]);
        expect(new Intl.DurationFormat("de", { style: "short" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1 J" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 Mon." },
            { type: "literal", value: ", " },
            { type: "element", value: "3 Wo." },
            { type: "literal", value: ", " },
            { type: "element", value: "3 Tg." },
            { type: "literal", value: ", " },
            { type: "element", value: "4 Std." },
            { type: "literal", value: ", " },
            { type: "element", value: "5 Min." },
            { type: "literal", value: ", " },
            { type: "element", value: "6 Sek." },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 μs" },
            { type: "literal", value: " und " },
            { type: "element", value: "9 ns" },
        ]);
        expect(new Intl.DurationFormat("de", { style: "narrow" }).formatToParts(duration)).toEqual([
            { type: "element", value: "1 J" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 M" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 W" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 T" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 Std." },
            { type: "literal", value: ", " },
            { type: "element", value: "5 Min." },
            { type: "literal", value: ", " },
            { type: "element", value: "6 Sek." },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: ", " },
            { type: "element", value: "8 μs" },
            { type: "literal", value: " und " },
            { type: "element", value: "9 ns" },
        ]);
        expect(new Intl.DurationFormat("de", { style: "digital" }).formatToParts(duration)).toEqual(
            [
                { type: "element", value: "1 J" },
                { type: "literal", value: ", " },
                { type: "element", value: "2 Mon." },
                { type: "literal", value: ", " },
                { type: "element", value: "3 Wo." },
                { type: "literal", value: ", " },
                { type: "element", value: "3 Tg." },
                { type: "literal", value: " und " },
                { type: "element", value: "4:05:06" },
            ]
        );
        expect(
            new Intl.DurationFormat("de", {
                style: "narrow",
                nanoseconds: "numeric",
                fractionalDigits: 3,
            }).formatToParts(duration)
        ).toEqual([
            { type: "element", value: "1 J" },
            { type: "literal", value: ", " },
            { type: "element", value: "2 M" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 W" },
            { type: "literal", value: ", " },
            { type: "element", value: "3 T" },
            { type: "literal", value: ", " },
            { type: "element", value: "4 Std." },
            { type: "literal", value: ", " },
            { type: "element", value: "5 Min." },
            { type: "literal", value: ", " },
            { type: "element", value: "6 Sek." },
            { type: "literal", value: ", " },
            { type: "element", value: "7 ms" },
            { type: "literal", value: " und " },
            { type: "element", value: "8,009 μs" },
        ]);
    });
});

describe("errors", () => {
    test("non-object duration records", () => {
        expect(() => {
            new Intl.DurationFormat().formatToParts("hello");
        }).toThrowWithMessage(RangeError, "is not an object");

        [-100, Infinity, NaN, 152n, Symbol("foo"), true, null, undefined].forEach(value => {
            expect(() => {
                new Intl.DurationFormat().formatToParts(value);
            }).toThrowWithMessage(TypeError, "is not an object");
        });
    });

    test("empty duration record", () => {
        expect(() => {
            new Intl.DurationFormat().formatToParts({});
        }).toThrowWithMessage(TypeError, "Invalid duration-like object");

        expect(() => {
            new Intl.DurationFormat().formatToParts({ foo: 123 });
        }).toThrowWithMessage(TypeError, "Invalid duration-like object");
    });

    test("non-integral duration fields", () => {
        [
            "years",
            "months",
            "weeks",
            "days",
            "hours",
            "minutes",
            "seconds",
            "milliseconds",
            "microseconds",
            "nanoseconds",
        ].forEach(field => {
            expect(() => {
                new Intl.DurationFormat().formatToParts({ [field]: 1.5 });
            }).toThrowWithMessage(
                RangeError,
                `Invalid value for duration property '${field}': must be an integer, got 1.5`
            );

            expect(() => {
                new Intl.DurationFormat().formatToParts({ [field]: -Infinity });
            }).toThrowWithMessage(
                RangeError,
                `Invalid value for duration property '${field}': must be an integer, got -Infinity`
            );
        });
    });

    test("inconsistent field signs", () => {
        expect(() => {
            new Intl.DurationFormat().formatToParts({ years: 1, months: -1 });
        }).toThrowWithMessage(RangeError, "Invalid duration-like object");
    });
});
