describe("correct behavior", () => {
    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(0n, timeZone);
        expect(zonedDateTime.timeZone).toBe(timeZone);
    });
});

test("errors", () => {
    test("this value must be a Temporal.ZonedDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.ZonedDateTime.prototype, "timeZone", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.ZonedDateTime");
    });
});
