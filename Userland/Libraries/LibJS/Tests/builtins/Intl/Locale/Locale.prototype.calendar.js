describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.calendar;
        }).toThrowWithMessage(TypeError, "Not a Intl.Locale object");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(new Intl.Locale("en").calendar).toBeUndefined();
        expect(new Intl.Locale("en-u-ca-abc").calendar).toBe("abc");
        expect(new Intl.Locale("en", { calendar: "abc" }).calendar).toBe("abc");
        expect(new Intl.Locale("en-u-ca-abc", { calendar: "def" }).calendar).toBe("def");
    });
});
