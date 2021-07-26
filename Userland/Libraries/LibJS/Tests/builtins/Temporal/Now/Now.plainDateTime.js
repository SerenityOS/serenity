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

    test("custom time zone", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainDateTime = Temporal.Now.plainDateTime(calendar);
        const plainDateTimeWithOffset = Temporal.Now.plainDateTime(calendar, timeZone);
        // Yes, this will fail if a day, month, or year change happens between the above two lines :^)
        // FIXME: enable these once the getters are implemented
        // expect(plainDateTimeWithOffset.year).toBe(plainDateTime.year);
        // expect(plainDateTimeWithOffset.month).toBe(plainDateTime.month);
        // expect(plainDateTimeWithOffset.day).toBe(plainDateTime.day + 1);
        // expect(plainDateTimeWithOffset.hour).not.toBe(plainDateTime.hour);
        // expect(plainDateTimeWithOffset.minute).not.toBe(plainDateTime.minute);
        // expect(plainDateTimeWithOffset.second).not.toBe(plainDateTime.second);
        // expect(plainDateTimeWithOffset.millisecond).not.toBe(plainDateTime.millisecond);
        // microsecond, and nanosecond not checked here as they could easily be the same for both
    });
});
