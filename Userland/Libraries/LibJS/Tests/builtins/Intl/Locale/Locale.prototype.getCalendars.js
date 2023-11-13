describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.getCalendars();
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        expect(Array.isArray(new Intl.Locale("en").getCalendars())).toBeTrue();
        expect(new Intl.Locale("en").getCalendars()).toEqual(["gregory"]);

        expect(Array.isArray(new Intl.Locale("ar").getCalendars())).toBeTrue();
        expect(new Intl.Locale("ar").getCalendars()).toEqual(["gregory"]);
    });

    test("extension keyword overrides default data", () => {
        expect(new Intl.Locale("en-u-ca-islamicc").getCalendars()).toEqual(["islamic-civil"]);
        expect(new Intl.Locale("en", { calendar: "dangi" }).getCalendars()).toEqual(["dangi"]);

        expect(new Intl.Locale("ar-u-ca-ethiopic-amete-alem").getCalendars()).toEqual(["ethioaa"]);
        expect(new Intl.Locale("ar", { calendar: "hebrew" }).getCalendars()).toEqual(["hebrew"]);

        // Invalid calendars also take precedence.
        expect(new Intl.Locale("en-u-ca-ladybird").getCalendars()).toEqual(["ladybird"]);
        expect(new Intl.Locale("en", { calendar: "ladybird" }).getCalendars()).toEqual([
            "ladybird",
        ]);
    });
});
