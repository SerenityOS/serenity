describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.collations;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").collations)).toBeTrue();
        expect(new Intl.Locale("en").collations).toEqual(["default"]);

        expect(Array.isArray(new Intl.Locale("ar").collations)).toBeTrue();
        expect(new Intl.Locale("ar").collations).toEqual(["default"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-co-compat").collations).toEqual(["compat"]);
        expect(new Intl.Locale("en", { collation: "compat" }).collations).toEqual(["compat"]);

        expect(new Intl.Locale("ar-u-co-reformed").collations).toEqual(["reformed"]);
        expect(new Intl.Locale("ar", { collation: "reformed" }).collations).toEqual(["reformed"]);

        // Invalid collations also take precedence.
        expect(new Intl.Locale("en-u-co-ladybird").collations).toEqual(["ladybird"]);
        expect(new Intl.Locale("en", { collation: "ladybird" }).collations).toEqual(["ladybird"]);
    });
});
