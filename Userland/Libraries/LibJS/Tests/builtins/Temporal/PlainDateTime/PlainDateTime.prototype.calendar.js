describe("correct behavior", () => {
    test("basic functionality", () => {
        const calendar = { hello: "friends" };
        const plainDateTime = new Temporal.PlainDateTime(0, 1, 1, 0, 0, 0, 0, 0, 0, calendar);
        expect(plainDateTime.calendar).toBe(calendar);
    });
});

test("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDateTime.prototype, "calendar", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDateTime");
    });
});
