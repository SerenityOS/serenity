describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.getNumberingSystems();
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").getNumberingSystems())).toBeTrue();
        expect(new Intl.Locale("en").getNumberingSystems()).toEqual(["latn"]);

        expect(Array.isArray(new Intl.Locale("ar").getNumberingSystems())).toBeTrue();
        expect(new Intl.Locale("ar").getNumberingSystems()).toEqual(["arab", "latn"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-nu-deva").getNumberingSystems()).toEqual(["deva"]);
        expect(new Intl.Locale("en", { numberingSystem: "deva" }).getNumberingSystems()).toEqual([
            "deva",
        ]);

        expect(new Intl.Locale("ar-u-nu-bali").getNumberingSystems()).toEqual(["bali"]);
        expect(new Intl.Locale("ar", { numberingSystem: "bali" }).getNumberingSystems()).toEqual([
            "bali",
        ]);

        // Invalid numberingSystems also take precedence.
        expect(new Intl.Locale("en-u-nu-ladybird").getNumberingSystems()).toEqual(["ladybird"]);
        expect(
            new Intl.Locale("en", { numberingSystem: "ladybird" }).getNumberingSystems()
        ).toEqual(["ladybird"]);
    });
});
