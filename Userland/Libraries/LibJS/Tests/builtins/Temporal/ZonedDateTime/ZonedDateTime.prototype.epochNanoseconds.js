describe("correct behavior", () => {
    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(1625614921000000000n, timeZone);
        expect(zonedDateTime.epochNanoseconds).toBe(1625614921000000000n);
    });
});

test("errors", () => {
    test("this value must be a Temporal.ZonedDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.ZonedDateTime.prototype, "epochNanoseconds", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.ZonedDateTime");
    });
});
