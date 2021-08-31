describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.prototype.dateFromFields).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const date = calendar.dateFromFields({ year: 2000, month: 5, day: 2 });
        expect(date.calendar).toBe(calendar);
    });
});
