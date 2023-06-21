describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Instant.prototype.since).toHaveLength(1);
    });

    test("basic functionality", () => {
        const instant1 = new Temporal.Instant(1625614920000000000n);
        const instant2 = new Temporal.Instant(0n);
        expect(instant1.since(instant2).seconds).toBe(1625614920);
        expect(instant1.since(instant2, { largestUnit: "hour" }).hours).toBe(451559);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.Instant object", () => {
        expect(() => {
            Temporal.Instant.prototype.since.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.Instant");
    });
});
