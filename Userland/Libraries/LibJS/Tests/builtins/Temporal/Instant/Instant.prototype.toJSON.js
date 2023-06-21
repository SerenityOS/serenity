describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.Instant.prototype.toJSON).toHaveLength(0);
    });

    test("basic functionality", () => {
        const instant = new Temporal.Instant(1625614921123456789n);
        expect(instant.toJSON()).toBe("2021-07-06T23:42:01.123456789Z");
    });
});

describe("errors", () => {
    test("this value must be a Temporal.Instant object", () => {
        expect(() => {
            Temporal.Instant.prototype.toJSON.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.Instant");
    });
});
