describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Now.plainDateTime).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainDateTime = Temporal.Now.plainDateTime(calendar);
        expect(plainDateTime).toBeInstanceOf(Temporal.PlainDateTime);
        expect(plainDateTime.calendar).toBe(calendar);
    });

    const plainDateTimeToEpochSeconds = plainDateTime =>
        (plainDateTime.year - 1970) * 31_556_952 +
        plainDateTime.dayOfYear * 86_400 +
        plainDateTime.hour * 3_600 +
        plainDateTime.minute * 60 +
        plainDateTime.second +
        plainDateTime.millisecond / 1_000 +
        plainDateTime.microsecond / 1_000_000 +
        plainDateTime.nanosecond / 1_000_000_000;

    let timeZoneTested = false;

    // Note: We test both positive and negative timezones because one might cross a year boundary.
    //       Since a year does not have a fixed amount of seconds because it can be a leap year,
    //       we cannot have a correct constant for seconds per year which is always correct.
    //       However, by assuming years are at least 2 days long we can simply try the positive
    //       and negative timezones and skip one if we jump the year. To ensure at least one is
    //       tested we have the timeZoneTested which is only set to true if one of the tests passed.

    test("custom time zone positive", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };

        const plainDateTime = Temporal.Now.plainDateTime(calendar, "UTC");
        const plainDateTimeWithOffset = Temporal.Now.plainDateTime(calendar, timeZone);

        if (plainDateTime.year !== plainDateTimeWithOffset.year) return;

        // Let's hope the duration between the above two lines is less than a second :^)
        const differenceSeconds =
            plainDateTimeToEpochSeconds(plainDateTimeWithOffset) -
            plainDateTimeToEpochSeconds(plainDateTime);
        expect(Math.floor(differenceSeconds)).toBe(86400);
        timeZoneTested = true;
    });

    test("custom time zone negative", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const timeZone = {
            getOffsetNanosecondsFor() {
                return -86400000000000;
            },
        };

        const plainDateTime = Temporal.Now.plainDateTime(calendar, "UTC");
        const plainDateTimeWithOffset = Temporal.Now.plainDateTime(calendar, timeZone);

        if (plainDateTime.year !== plainDateTimeWithOffset.year) return;

        // Let's hope the duration between the above two lines is less than a second :^)
        const differenceSeconds =
            plainDateTimeToEpochSeconds(plainDateTimeWithOffset) -
            plainDateTimeToEpochSeconds(plainDateTime);
        expect(Math.floor(differenceSeconds)).toBe(-86400);
        timeZoneTested = true;
    });

    expect(timeZoneTested).toBeTrue();
});

describe("errors", () => {
    test("custom time zone doesn't have a getOffsetNanosecondsFor function", () => {
        expect(() => {
            Temporal.Now.plainDateTime({}, {});
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
