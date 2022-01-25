describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.RelativeTimeFormat();
        }).toThrowWithMessage(
            TypeError,
            "Intl.RelativeTimeFormat constructor must be called with 'new'"
        );
    });
});

describe("normal behavior", () => {
    test("length is 0", () => {
        expect(Intl.RelativeTimeFormat).toHaveLength(0);
    });
});
