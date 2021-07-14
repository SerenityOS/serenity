describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.Calendar.prototype.toJSON).toHaveLength(0);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(calendar.toJSON()).toBe("iso8601");
    });

    test("works with any this value", () => {
        expect(Temporal.Calendar.prototype.toJSON.call("foo")).toBe("foo");
    });
});
