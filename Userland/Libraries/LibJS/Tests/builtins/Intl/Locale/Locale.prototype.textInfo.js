describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.textInfo;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        const textInfo = new Intl.Locale("en").textInfo;

        expect(textInfo).toBeDefined();
        expect(Object.getPrototypeOf(textInfo)).toBe(Object.prototype);

        expect(textInfo.direction).toBeDefined();
        expect(Object.getPrototypeOf(textInfo.direction)).toBe(String.prototype);

        expect(textInfo.direction).toBe("ltr");
        expect(new Intl.Locale("ar").textInfo.direction).toBe("rtl");
    });

    test("fallback to ltr", () => {
        expect(new Intl.Locale("xx").textInfo.direction).toBe("ltr");
    });
});
