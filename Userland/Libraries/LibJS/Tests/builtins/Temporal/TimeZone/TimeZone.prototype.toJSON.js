describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.TimeZone.prototype.toJSON).toHaveLength(0);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        expect(timeZone.toJSON()).toBe("UTC");
    });

    test("works with any this value", () => {
        expect(Temporal.TimeZone.prototype.toJSON.call("foo")).toBe("foo");
    });
});
