describe("correct behavior", () => {
    test("basic functionality", () => {
        const duration = new Temporal.Duration(0, 0, 0, 0, 0, 0, 0, 123);
        expect(duration.milliseconds).toBe(123);
    });
});

test("errors", () => {
    test("this value must be a Temporal.Duration object", () => {
        expect(() => {
            Reflect.get(Temporal.Duration.prototype, "milliseconds", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.Duration");
    });
});
