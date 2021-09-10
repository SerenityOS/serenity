describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainMonthDay.from).toHaveLength(1);
    });

    test("PlainDate instance argument", () => {
        const plainDate = new Temporal.PlainDate(2021, 7, 6);
        const plainMonthDay = Temporal.PlainMonthDay.from(plainDate);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);
    });

    test("PlainMonthDay instance argument", () => {
        const plainMonthDay_ = new Temporal.PlainMonthDay(7, 6);
        const plainMonthDay = Temporal.PlainMonthDay.from(plainMonthDay_);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);
    });

    test("ZonedDateTime instance argument", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(1625614921000000000n, timeZone);
        const plainMonthDay = Temporal.PlainMonthDay.from(zonedDateTime);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);
    });

    test("fields object argument", () => {
        const object = {
            month: 7,
            day: 6,
        };
        const plainMonthDay = Temporal.PlainMonthDay.from(object);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);
    });

    // Un-skip once ParseISODateTime, ToTemporalMonthDay & ParseTemporalMonthDayString are fully implemented
    test.skip("PlainMonthDay string argument", () => {
        const plainMonthDay = Temporal.PlainMonthDay.from("2021-07-06T23:42:01Z");
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);
    });
});

describe("errors", () => {
    test("missing fields", () => {
        expect(() => {
            Temporal.PlainMonthDay.from({});
        }).toThrowWithMessage(TypeError, "Required property month is missing or undefined");
        expect(() => {
            Temporal.PlainMonthDay.from({ month: 1 });
        }).toThrowWithMessage(TypeError, "Required property day is missing or undefined");
        expect(() => {
            Temporal.PlainMonthDay.from({ day: 1 });
        }).toThrowWithMessage(TypeError, "Required property month is missing or undefined");
    });
});
