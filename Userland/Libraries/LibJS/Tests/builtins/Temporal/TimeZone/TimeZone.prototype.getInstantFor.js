describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.prototype.getInstantFor).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 6, 18, 14, 47);
        const instant = timeZone.getInstantFor(plainDateTime);
        expect(instant).toBeInstanceOf(Temporal.Instant);
        expect(instant.epochNanoseconds).toBe(1625595287000000000n);
    });

    test("custom offset", () => {
        const timeZone = new Temporal.TimeZone("+01:30");
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 6, 18, 14, 47);
        const instant = timeZone.getInstantFor(plainDateTime);
        expect(instant).toBeInstanceOf(Temporal.Instant);
        expect(instant.epochNanoseconds).toBe(1625589887000000000n);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.TimeZone object", () => {
        expect(() => {
            Temporal.TimeZone.prototype.getInstantFor.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.TimeZone");
    });
});
