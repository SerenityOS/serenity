describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.prototype.getPlainDateTimeFor).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const instant = Temporal.Instant.fromEpochSeconds(123456789);
        const plainDateTime = timeZone.getPlainDateTimeFor(instant);
        expect(plainDateTime.year).toBe(1973);
        expect(plainDateTime.month).toBe(11);
        expect(plainDateTime.day).toBe(29);
        expect(plainDateTime.hour).toBe(21);
        expect(plainDateTime.minute).toBe(33);
        expect(plainDateTime.second).toBe(9);
        expect(plainDateTime.millisecond).toBe(0);
        expect(plainDateTime.microsecond).toBe(0);
        expect(plainDateTime.nanosecond).toBe(0);
        expect(plainDateTime.calendar.id).toBe("iso8601");
    });

    test("custom calendar", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const instant = new Temporal.Instant(0n);
        const calendar = new Temporal.Calendar("iso8601");
        const plainDateTime = timeZone.getPlainDateTimeFor(instant, calendar);
        expect(plainDateTime.calendar).toBe(calendar);
    });

    test("non-TimeZone this value", () => {
        const timeZoneLike = {
            getOffsetNanosecondsFor() {
                return 123;
            },
        };
        const instant = new Temporal.Instant(0n);
        const plainDateTime = Temporal.TimeZone.prototype.getPlainDateTimeFor.call(
            timeZoneLike,
            instant
        );
        expect(plainDateTime.year).toBe(1970);
        expect(plainDateTime.month).toBe(1);
        expect(plainDateTime.day).toBe(1);
        expect(plainDateTime.hour).toBe(0);
        expect(plainDateTime.minute).toBe(0);
        expect(plainDateTime.second).toBe(0);
        expect(plainDateTime.millisecond).toBe(0);
        expect(plainDateTime.microsecond).toBe(0);
        expect(plainDateTime.nanosecond).toBe(123);
    });
});

describe("errors", () => {
    test("custom time zone doesn't have a getOffsetNanosecondsFor function", () => {
        const instant = new Temporal.Instant(1n);
        expect(() => {
            Temporal.TimeZone.prototype.getPlainDateTimeFor.call({}, instant);
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
