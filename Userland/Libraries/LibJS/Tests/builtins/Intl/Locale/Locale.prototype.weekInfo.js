describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.weekInfo;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        const weekInfo = new Intl.Locale("en-US").weekInfo;

        expect(weekInfo).toBeDefined();
        expect(Object.getPrototypeOf(weekInfo)).toBe(Object.prototype);

        expect(weekInfo.firstDay).toBeDefined();
        expect(Object.getPrototypeOf(weekInfo.firstDay)).toBe(Number.prototype);
        expect(weekInfo.firstDay).toBe(7);

        expect(weekInfo.weekend).toBeDefined();
        expect(Array.isArray(weekInfo.weekend)).toBeTrue();
        expect(weekInfo.weekend).toEqual([6, 7]);

        expect(weekInfo.minimalDays).toBeDefined();
        expect(Object.getPrototypeOf(weekInfo.minimalDays)).toBe(Number.prototype);
        expect(weekInfo.minimalDays).toBe(1);
    });

    test("regions with CLDR-specified firstDay", () => {
        expect(new Intl.Locale("en-AG").weekInfo.firstDay).toBe(7);
        expect(new Intl.Locale("en-SY").weekInfo.firstDay).toBe(6);
        expect(new Intl.Locale("en-MV").weekInfo.firstDay).toBe(5);
    });

    test("firstDay falls back to default region 001", () => {
        expect(new Intl.Locale("en-AC").weekInfo.firstDay).toBe(1);
    });

    test("regions with CLDR-specified weekend", () => {
        expect(new Intl.Locale("en-AF").weekInfo.weekend).toEqual([4, 5]);
        expect(new Intl.Locale("en-IN").weekInfo.weekend).toEqual([7]);
        expect(new Intl.Locale("en-YE").weekInfo.weekend).toEqual([5, 6]);
    });

    test("weekend falls back to default region 001", () => {
        expect(new Intl.Locale("en-AC").weekInfo.weekend).toEqual([6, 7]);
    });

    test("regions with CLDR-specified minimalDays", () => {
        expect(new Intl.Locale("en-AD").weekInfo.minimalDays).toBe(4);
        expect(new Intl.Locale("en-CZ").weekInfo.minimalDays).toBe(4);
    });

    test("minimalDays falls back to default region 001", () => {
        expect(new Intl.Locale("en-AC").weekInfo.minimalDays).toBe(1);
    });

    test("likely regional subtags are added to locales without a region", () => {
        const defaultRegion = new Intl.Locale("en-001").weekInfo;

        // "en" expands to "en-US" when likely subtags are added.
        const en = new Intl.Locale("en").weekInfo;
        const enUS = new Intl.Locale("en-US").weekInfo;

        expect(en).toEqual(enUS);
        expect(en).not.toEqual(defaultRegion);
    });
});
