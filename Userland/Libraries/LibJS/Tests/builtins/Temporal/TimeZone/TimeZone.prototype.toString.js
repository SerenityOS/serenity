describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.TimeZone.prototype.toString).toHaveLength(0);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        expect(timeZone.toString()).toBe("UTC");
    });
});

test("errors", () => {
    test("this value must be a Temporal.TimeZone object", () => {
        expect(() => {
            Temporal.TimeZone.prototype.toString.call("foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.TimeZone");
    });
});
