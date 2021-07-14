describe("correct behavior", () => {
    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(calendar.id).toBe("iso8601");
    });

    test("works with any this value", () => {
        expect(Reflect.get(Temporal.Calendar.prototype, "id", "foo")).toBe("foo");
    });
});
