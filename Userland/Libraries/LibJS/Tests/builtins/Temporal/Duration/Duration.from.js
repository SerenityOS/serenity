const expectDurationOneToTen = duration => {
    expect(duration.years).toBe(1);
    expect(duration.months).toBe(2);
    expect(duration.weeks).toBe(3);
    expect(duration.days).toBe(4);
    expect(duration.hours).toBe(5);
    expect(duration.minutes).toBe(6);
    expect(duration.seconds).toBe(7);
    expect(duration.milliseconds).toBe(8);
    expect(duration.microseconds).toBe(9);
    expect(duration.nanoseconds).toBe(10);
};

describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Duration.from).toHaveLength(1);
    });

    test("Duration instance argument", () => {
        const duration = Temporal.Duration.from(
            new Temporal.Duration(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        );
        expectDurationOneToTen(duration);
    });

    test("Duration-like object argument", () => {
        const duration = Temporal.Duration.from({
            years: 1,
            months: 2,
            weeks: 3,
            days: 4,
            hours: 5,
            minutes: 6,
            seconds: 7,
            milliseconds: 8,
            microseconds: 9,
            nanoseconds: 10,
        });
        expectDurationOneToTen(duration);
    });

    // Un-skip once ParseTemporalDurationString is implemented
    test.skip("Duration string argument", () => {
        const duration = Temporal.Duration.from("TODO");
        expectDurationOneToTen(duration);
    });
});

describe("errors", () => {
    test("Invalid duration-like object", () => {
        expect(() => {
            Temporal.Duration.from({});
        }).toThrowWithMessage(TypeError, "Invalid duration-like object");
    });

    test("Invalid duration property value", () => {
        expect(() => {
            Temporal.Duration.from({ years: 1.23 });
        }).toThrowWithMessage(
            RangeError,
            "Invalid value for duration property 'years': must be an integer, got 1.2" // ...29999999999999 - let's not include that in the test :^)
        );
        expect(() => {
            Temporal.Duration.from({ years: "foo" });
        }).toThrowWithMessage(
            RangeError,
            "Invalid value for duration property 'years': must be an integer, got NaN"
        );
    });
});
