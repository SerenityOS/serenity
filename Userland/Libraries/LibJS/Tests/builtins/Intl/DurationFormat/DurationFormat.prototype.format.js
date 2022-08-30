describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Intl.DurationFormat.prototype.format).toHaveLength(1);
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
        expect(new Intl.DurationFormat().format(duration)).toBe(
            "1 yr, 2 mths, 3 wks, 3 days, 4 hr, 5 min, 6 sec, 7 ms, 8 μs, 9 ns"
        );
        expect(new Intl.DurationFormat("en").format(duration)).toBe(
            "1 yr, 2 mths, 3 wks, 3 days, 4 hr, 5 min, 6 sec, 7 ms, 8 μs, 9 ns"
        );
        expect(new Intl.DurationFormat("en", { style: "long" }).format(duration)).toBe(
            "1 year, 2 months, 3 weeks, 3 days, 4 hours, 5 minutes, 6 seconds, 7 milliseconds, 8 microseconds, 9 nanoseconds"
        );
        expect(new Intl.DurationFormat("en", { style: "short" }).format(duration)).toBe(
            "1 yr, 2 mths, 3 wks, 3 days, 4 hr, 5 min, 6 sec, 7 ms, 8 μs, 9 ns"
        );
        expect(new Intl.DurationFormat("en", { style: "narrow" }).format(duration)).toBe(
            "1y 2m 3w 3d 4h 5m 6s 7ms 8μs 9ns"
        );
        expect(new Intl.DurationFormat("en", { style: "digital" }).format(duration)).toBe(
            "1y 2m 3w 3d 4:05:06"
        );
        expect(
            new Intl.DurationFormat("en", {
                style: "narrow",
                nanoseconds: "numeric",
                fractionalDigits: 3,
            }).format(duration)
        ).toBe("1y 2m 3w 3d 4h 5m 6s 7ms 8.009μs");

        expect(new Intl.DurationFormat("de", { style: "long" }).format(duration)).toBe(
            "1 Jahr, 2 Monate, 3 Wochen, 3 Tage, 4 Stunden, 5 Minuten, 6 Sekunden, 7 Millisekunden, 8 Mikrosekunden und 9 Nanosekunden"
        );
        expect(new Intl.DurationFormat("de", { style: "short" }).format(duration)).toBe(
            "1 J, 2 Mon., 3 Wo., 3 Tg., 4 Std., 5 Min., 6 Sek., 7 ms, 8 μs und 9 ns"
        );
        expect(new Intl.DurationFormat("de", { style: "narrow" }).format(duration)).toBe(
            "1 J, 2 M, 3 W, 3 T, 4 Std., 5 Min., 6 Sek., 7 ms, 8 μs und 9 ns"
        );
        expect(new Intl.DurationFormat("de", { style: "digital" }).format(duration)).toBe(
            "1 J, 2 M, 3 W, 3 T und 4:05:06"
        );
        expect(
            new Intl.DurationFormat("de", {
                style: "narrow",
                nanoseconds: "numeric",
                fractionalDigits: 3,
            }).format(duration)
        ).toBe("1 J, 2 M, 3 W, 3 T, 4 Std., 5 Min., 6 Sek., 7 ms und 8,009 μs");
    });
});

describe("errors", () => {
    test("non-object duration records", () => {
        [-100, Infinity, NaN, "hello", 152n, Symbol("foo")].forEach(value => {
            expect(() => {
                new Intl.DurationFormat().format(value);
            }).toThrowWithMessage(TypeError, "is not an object");
        });
    });

    test("empty duration record", () => {
        expect(() => {
            new Intl.DurationFormat().format({});
        }).toThrowWithMessage(TypeError, "Invalid duration-like object");

        expect(() => {
            new Intl.DurationFormat().format({ foo: 123 });
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
                new Intl.DurationFormat().format({ [field]: 1.5 });
            }).toThrowWithMessage(
                RangeError,
                `Invalid value for duration property '${field}': must be an integer, got 1.5`
            );

            expect(() => {
                new Intl.DurationFormat().format({ [field]: -Infinity });
            }).toThrowWithMessage(
                RangeError,
                `Invalid value for duration property '${field}': must be an integer, got -Infinity`
            );
        });
    });

    test("inconsistent field signs", () => {
        expect(() => {
            new Intl.DurationFormat().format({ years: 1, months: -1 });
        }).toThrowWithMessage(RangeError, "Invalid duration-like object");
    });
});
