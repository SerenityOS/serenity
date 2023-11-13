describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.getCollations();
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").getCollations())).toBeTrue();
        expect(new Intl.Locale("en").getCollations()).toEqual(["default"]);

        expect(Array.isArray(new Intl.Locale("ar").getCollations())).toBeTrue();
        expect(new Intl.Locale("ar").getCollations()).toEqual(["default"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-co-compat").getCollations()).toEqual(["compat"]);
        expect(new Intl.Locale("en", { collation: "compat" }).getCollations()).toEqual(["compat"]);

        expect(new Intl.Locale("ar-u-co-reformed").getCollations()).toEqual(["reformed"]);
        expect(new Intl.Locale("ar", { collation: "reformed" }).getCollations()).toEqual([
            "reformed",
        ]);

        // Invalid getCollations() also take precedence.
        expect(new Intl.Locale("en-u-co-ladybird").getCollations()).toEqual(["ladybird"]);
        expect(new Intl.Locale("en", { collation: "ladybird" }).getCollations()).toEqual([
            "ladybird",
        ]);
    });
});
