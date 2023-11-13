describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.getHourCycles();
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").getHourCycles())).toBeTrue();
        expect(new Intl.Locale("en").getHourCycles()).toContain("h12");

        expect(Array.isArray(new Intl.Locale("ha").getHourCycles())).toBeTrue();
        expect(new Intl.Locale("ha").getHourCycles()).toContain("h23");
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-hc-h24").getHourCycles()).toEqual(["h24"]);
        expect(new Intl.Locale("en", { hourCycle: "h24" }).getHourCycles()).toEqual(["h24"]);

        expect(new Intl.Locale("ar-u-hc-h24").getHourCycles()).toEqual(["h24"]);
        expect(new Intl.Locale("ar", { hourCycle: "h24" }).getHourCycles()).toEqual(["h24"]);

        // Invalid hourCycles also take precedence when specified in the locale string. Unlike other
        // properties, Locale("en", { hourCycle: "ladybird" }) will explicitly throw.
        expect(new Intl.Locale("en-u-hc-ladybird").getHourCycles()).toEqual(["ladybird"]);
    });
});
