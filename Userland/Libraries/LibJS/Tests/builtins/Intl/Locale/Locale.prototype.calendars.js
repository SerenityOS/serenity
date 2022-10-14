describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.calendars;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").calendars)).toBeTrue();
        expect(new Intl.Locale("en").calendars).toEqual(["gregory"]);

        expect(Array.isArray(new Intl.Locale("ar").calendars)).toBeTrue();
        expect(new Intl.Locale("ar").calendars).toEqual(["gregory"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-ca-islamicc").calendars).toEqual(["islamic-civil"]);
        expect(new Intl.Locale("en", { calendar: "dangi" }).calendars).toEqual(["dangi"]);

        expect(new Intl.Locale("ar-u-ca-ethiopic-amete-alem").calendars).toEqual(["ethioaa"]);
        expect(new Intl.Locale("ar", { calendar: "hebrew" }).calendars).toEqual(["hebrew"]);

        // Invalid calendars also take precedence.
        expect(new Intl.Locale("en-u-ca-ladybird").calendars).toEqual(["ladybird"]);
        expect(new Intl.Locale("en", { calendar: "ladybird" }).calendars).toEqual(["ladybird"]);
    });
});
