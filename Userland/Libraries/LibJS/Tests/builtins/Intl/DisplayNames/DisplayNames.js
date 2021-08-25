describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.DisplayNames();
        }).toThrowWithMessage(TypeError, "Intl.DisplayNames constructor must be called with 'new'");
    });
});

describe("normal behavior", () => {
    test("length is 2", () => {
        expect(Intl.DisplayNames).toHaveLength(2);
    });
});
