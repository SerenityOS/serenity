describe("correct behavior", () => {
    test("basic functionality", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 30, 1, 4);
        expect(plainDateTime.minute).toBe(4);
    });
});

test("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDateTime.prototype, "minute", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDateTime");
    });
});
