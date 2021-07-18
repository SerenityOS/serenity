describe("correct behavior", () => {
    test("basic functionality", () => {
        const calendar = { hello: "friends" };
        const plain_date = new Temporal.PlainDate(1, 1, 1, calendar);
        expect(plain_date.calendar).toBe(calendar);
    });
});

test("errors", () => {
    test("this value must be a Temporal.Duration object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDate.prototype, "calendar", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDate");
    });
});
