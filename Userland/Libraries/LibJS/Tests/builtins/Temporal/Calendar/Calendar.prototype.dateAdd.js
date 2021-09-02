describe("correct behavior", () => {
    test("length is 2", () => {
        expect(Temporal.Calendar.prototype.dateAdd).toHaveLength(2);
    });

    // Asserts with TODO() since addition of partial balance_duration()
    test.skip("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainDate = new Temporal.PlainDate(1970, 1, 1);
        const duration = new Temporal.Duration(1, 2, 3, 4);
        const newPlainDate = calendar.dateAdd(plainDate, duration);
        expect(newPlainDate.year).toBe(1971);
        expect(newPlainDate.month).toBe(3);
        expect(newPlainDate.day).toBe(26);
    });
});
