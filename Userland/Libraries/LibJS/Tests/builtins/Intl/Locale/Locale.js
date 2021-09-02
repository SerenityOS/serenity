describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.Locale();
        }).toThrowWithMessage(TypeError, "Intl.Locale constructor must be called with 'new'");
    });
});

describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Intl.Locale).toHaveLength(1);
    });
});
