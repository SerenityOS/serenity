describe("correct behavior", () => {
    test("basic functionality", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 30);
        expect(plainDateTime.inLeapYear).toBeFalse();
    });
});

describe("errors", () => {
    test("this value must be a Temporal.PlainDateTime object", () => {
        expect(() => {
            Reflect.get(Temporal.PlainDateTime.prototype, "inLeapYear", "foo");
        }).toThrowWithMessage(TypeError, "Not a Temporal.PlainDateTime");
    });
});
