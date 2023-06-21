describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.numberingSystems;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").numberingSystems)).toBeTrue();
        expect(new Intl.Locale("en").numberingSystems).toEqual(["latn"]);

        expect(Array.isArray(new Intl.Locale("ar").numberingSystems)).toBeTrue();
        expect(new Intl.Locale("ar").numberingSystems).toEqual(["arab", "latn"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-nu-deva").numberingSystems).toEqual(["deva"]);
        expect(new Intl.Locale("en", { numberingSystem: "deva" }).numberingSystems).toEqual([
            "deva",
        ]);

        expect(new Intl.Locale("ar-u-nu-bali").numberingSystems).toEqual(["bali"]);
        expect(new Intl.Locale("ar", { numberingSystem: "bali" }).numberingSystems).toEqual([
            "bali",
        ]);

        // Invalid numberingSystems also take precedence.
        expect(new Intl.Locale("en-u-nu-ladybird").numberingSystems).toEqual(["ladybird"]);
        expect(new Intl.Locale("en", { numberingSystem: "ladybird" }).numberingSystems).toEqual([
            "ladybird",
        ]);
    });
});
