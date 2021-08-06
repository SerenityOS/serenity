describe("correct behavior", () => {
    test("basic functionality", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 29);
        expect(plainDateTime.month).toBe(7);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDateTime.prototype, "month", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDateTime");
    });
});
