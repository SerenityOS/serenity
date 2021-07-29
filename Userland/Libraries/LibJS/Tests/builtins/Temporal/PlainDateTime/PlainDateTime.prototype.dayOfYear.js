describe("correct behavior", () => {
    test("basic functionality", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 30);
        expect(plainDateTime.dayOfYear).toBe(211);
    });
});

test("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDateTime.prototype, "dayOfYear", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDateTime");
    });
});
