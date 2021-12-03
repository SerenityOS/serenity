describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainTime.from).toHaveLength(1);
    });

    test("PlainTime instance argument", () => {
        const plainTime = new Temporal.PlainTime(18, 45, 37, 1, 2, 3);
        const createdPlainTime = Temporal.PlainTime.from(plainTime);
        expect(createdPlainTime.hour).toBe(18);
        expect(createdPlainTime.minute).toBe(45);
        expect(createdPlainTime.second).toBe(37);
        expect(createdPlainTime.millisecond).toBe(1);
        expect(createdPlainTime.microsecond).toBe(2);
        expect(createdPlainTime.nanosecond).toBe(3);
    });

    test("PlainDateTime instance argument", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 8, 27, 18, 45, 37, 1, 2, 3);
        const createdPlainTime = Temporal.PlainTime.from(plainDateTime);
        expect(createdPlainTime.hour).toBe(18);
        expect(createdPlainTime.minute).toBe(45);
        expect(createdPlainTime.second).toBe(37);
        expect(createdPlainTime.millisecond).toBe(1);
        expect(createdPlainTime.microsecond).toBe(2);
        expect(createdPlainTime.nanosecond).toBe(3);
    });

    test("ZonedDateTime instance argument", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(1627318123456789000n, timeZone);
        const createdPlainTime = Temporal.PlainTime.from(zonedDateTime);
        expect(createdPlainTime.hour).toBe(16);
        expect(createdPlainTime.minute).toBe(48);
        expect(createdPlainTime.second).toBe(43);
        expect(createdPlainTime.millisecond).toBe(456);
        expect(createdPlainTime.microsecond).toBe(789);
        expect(createdPlainTime.nanosecond).toBe(0);
    });

    // Un-skip once ParseISODateTime & ParseTemporalTimeString are implemented
    test.skip("PlainTime string argument", () => {
        const createdPlainTime = Temporal.PlainTime.from("2021-08-27T18:44:11Z");
        expect(createdPlainTime.hour).toBe(18);
        expect(createdPlainTime.minute).toBe(44);
        expect(createdPlainTime.second).toBe(11);
        expect(createdPlainTime.millisecond).toBe(0);
        expect(createdPlainTime.microsecond).toBe(0);
        expect(createdPlainTime.nanosecond).toBe(0);
    });
});

describe("errors", () => {
    test("custom time zone doesn't have a getOffsetNanosecondsFor function", () => {
        const zonedDateTime = new Temporal.ZonedDateTime(0n, {});
        expect(() => {
            Temporal.PlainTime.from(zonedDateTime);
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
