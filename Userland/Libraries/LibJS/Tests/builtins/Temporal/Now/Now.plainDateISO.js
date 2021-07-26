describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.Now.plainDateISO).toHaveLength(0);
    });

    test("basic functionality", () => {
        const plainDate = Temporal.Now.plainDateISO();
        expect(plainDate).toBeInstanceOf(Temporal.PlainDate);
        expect(plainDate.calendar.id).toBe("iso8601");
    });

    test("custom time zone", () => {
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainDate = Temporal.Now.plainDateISO();
        const plainDateWithOffset = Temporal.Now.plainDateISO(timeZone);
        // Yes, this will fail if a day, month, or year change happens between the above two lines :^)
        expect(plainDateWithOffset.year).toBe(plainDate.year);
        expect(plainDateWithOffset.month).toBe(plainDate.month);
        expect(plainDateWithOffset.day).toBe(plainDate.day + 1);
    });
});
