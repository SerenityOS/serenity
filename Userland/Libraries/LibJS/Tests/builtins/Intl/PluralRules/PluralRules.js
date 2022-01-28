describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.PluralRules();
        }).toThrowWithMessage(TypeError, "Intl.PluralRules constructor must be called with 'new'");
    });
});

describe("normal behavior", () => {
    test("length is 0", () => {
        expect(Intl.PluralRules).toHaveLength(0);
    });
});
