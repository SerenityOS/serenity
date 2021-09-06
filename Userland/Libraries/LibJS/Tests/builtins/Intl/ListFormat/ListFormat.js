describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.ListFormat();
        }).toThrowWithMessage(TypeError, "Intl.ListFormat constructor must be called with 'new'");
    });
});

describe("normal behavior", () => {
    test("length is 0", () => {
        expect(Intl.ListFormat).toHaveLength(0);
    });
});
