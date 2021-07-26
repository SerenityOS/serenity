describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Now.plainDate).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainDate = Temporal.Now.plainDate(calendar);
        expect(plainDate).toBeInstanceOf(Temporal.PlainDate);
        expect(plainDate.calendar).toBe(calendar);
    });

    test("custom time zone", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainDate = Temporal.Now.plainDate(calendar);
        const plainDateWithOffset = Temporal.Now.plainDate(calendar, timeZone);
        // Yes, this will fail if a day, month, or year change happens between the above two lines :^)
        expect(plainDateWithOffset.year).toBe(plainDate.year);
        expect(plainDateWithOffset.month).toBe(plainDate.month);
        expect(plainDateWithOffset.day).toBe(plainDate.day + 1);
    });
});
