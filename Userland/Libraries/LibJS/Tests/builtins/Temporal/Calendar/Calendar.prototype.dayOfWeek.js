describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.prototype.dayOfWeek).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const date = new Temporal.PlainDate(2021, 7, 23);
        expect(calendar.dayOfWeek(date)).toBe(5);
    });
});
