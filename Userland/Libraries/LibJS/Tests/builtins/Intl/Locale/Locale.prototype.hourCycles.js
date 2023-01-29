describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.hourCycles;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").hourCycles)).toBeTrue();
        expect(new Intl.Locale("en").hourCycles).toContain("h12");

        expect(Array.isArray(new Intl.Locale("ha").hourCycles)).toBeTrue();
        expect(new Intl.Locale("ha").hourCycles).toContain("h23");
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-hc-h24").hourCycles).toEqual(["h24"]);
        expect(new Intl.Locale("en", { hourCycle: "h24" }).hourCycles).toEqual(["h24"]);

        expect(new Intl.Locale("ar-u-hc-h24").hourCycles).toEqual(["h24"]);
        expect(new Intl.Locale("ar", { hourCycle: "h24" }).hourCycles).toEqual(["h24"]);

        // Invalid hourCycles also take precedence when specified in the locale string. Unlike other
        // properties, Locale("en", { hourCycle: "ladybird" }) will explicitly throw.
        expect(new Intl.Locale("en-u-hc-ladybird").hourCycles).toEqual(["ladybird"]);
    });
});
