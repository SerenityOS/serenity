describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.prototype.getOffsetNanosecondsFor).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const instant = new Temporal.Instant(0n);
        expect(timeZone.getOffsetNanosecondsFor(instant)).toBe(0);
    });

    test("custom offset", () => {
        const timeZone = new Temporal.TimeZone("+01:30");
        const instant = new Temporal.Instant(0n);
        expect(timeZone.getOffsetNanosecondsFor(instant)).toBe(5400000000000);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.TimeZone object", () => {
        expect(() => {
            Temporal.TimeZone.prototype.getOffsetNanosecondsFor.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.TimeZone");
    });
});
