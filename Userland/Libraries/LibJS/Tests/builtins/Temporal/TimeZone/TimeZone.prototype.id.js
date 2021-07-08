describe("correct behavior", () => {
    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        expect(timeZone.id).toBe("UTC");
    });

    test("works with any this value", () => {
        expect(Reflect.get(Temporal.TimeZone.prototype, "id", "foo")).toBe("foo");
    });
});
