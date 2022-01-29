describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Intl.PluralRules.prototype.resolvedOptions).toHaveLength(0);
    });

    test("locale only contains relevant extension keys", () => {
        const en1 = new Intl.PluralRules("en-u-ca-islamicc");
        expect(en1.resolvedOptions().locale).toBe("en");

        const en2 = new Intl.PluralRules("en-u-nu-latn");
        expect(en2.resolvedOptions().locale).toBe("en");

        const en3 = new Intl.PluralRules("en-u-ca-islamicc-nu-latn");
        expect(en3.resolvedOptions().locale).toBe("en");
    });

    test("type", () => {
        const en1 = new Intl.PluralRules("en");
        expect(en1.resolvedOptions().type).toBe("cardinal");

        ["cardinal", "ordinal"].forEach(type => {
            const en2 = new Intl.PluralRules("en", { type: type });
            expect(en2.resolvedOptions().type).toBe(type);
        });
    });

    test("min integer digits", () => {
        const en1 = new Intl.PluralRules("en");
        expect(en1.resolvedOptions().minimumIntegerDigits).toBe(1);

        const en2 = new Intl.PluralRules("en", { minimumIntegerDigits: 5 });
        expect(en2.resolvedOptions().minimumIntegerDigits).toBe(5);
    });

    test("min/max fraction digits", () => {
        const en1 = new Intl.PluralRules("en", { minimumFractionDigits: 5 });
        expect(en1.resolvedOptions().minimumFractionDigits).toBe(5);
        expect(en1.resolvedOptions().maximumFractionDigits).toBe(5);
        expect(en1.resolvedOptions().minimumSignificantDigits).toBeUndefined();
        expect(en1.resolvedOptions().maximumSignificantDigits).toBeUndefined();

        const en2 = new Intl.PluralRules("en", { maximumFractionDigits: 5 });
        expect(en2.resolvedOptions().minimumFractionDigits).toBe(0);
        expect(en2.resolvedOptions().maximumFractionDigits).toBe(5);
        expect(en2.resolvedOptions().minimumSignificantDigits).toBeUndefined();
        expect(en2.resolvedOptions().maximumSignificantDigits).toBeUndefined();

        const en3 = new Intl.PluralRules("en", {
            minimumFractionDigits: 5,
            maximumFractionDigits: 10,
        });
        expect(en3.resolvedOptions().minimumFractionDigits).toBe(5);
        expect(en3.resolvedOptions().maximumFractionDigits).toBe(10);
        expect(en3.resolvedOptions().minimumSignificantDigits).toBeUndefined();
        expect(en3.resolvedOptions().maximumSignificantDigits).toBeUndefined();
    });

    test("min/max significant digits", () => {
        const en1 = new Intl.PluralRules("en", { minimumSignificantDigits: 5 });
        expect(en1.resolvedOptions().minimumFractionDigits).toBeUndefined();
        expect(en1.resolvedOptions().maximumFractionDigits).toBeUndefined();
        expect(en1.resolvedOptions().minimumSignificantDigits).toBe(5);
        expect(en1.resolvedOptions().maximumSignificantDigits).toBe(21);

        const en2 = new Intl.PluralRules("en", { maximumSignificantDigits: 5 });
        expect(en2.resolvedOptions().minimumFractionDigits).toBeUndefined();
        expect(en2.resolvedOptions().maximumFractionDigits).toBeUndefined();
        expect(en2.resolvedOptions().minimumSignificantDigits).toBe(1);
        expect(en2.resolvedOptions().maximumSignificantDigits).toBe(5);

        const en3 = new Intl.PluralRules("en", {
            minimumSignificantDigits: 5,
            maximumSignificantDigits: 10,
        });
        expect(en3.resolvedOptions().minimumFractionDigits).toBeUndefined();
        expect(en3.resolvedOptions().maximumFractionDigits).toBeUndefined();
        expect(en3.resolvedOptions().minimumSignificantDigits).toBe(5);
        expect(en3.resolvedOptions().maximumSignificantDigits).toBe(10);
    });

    test("plural categories", () => {
        // FIXME: Write better tests when this is implemented.
        const en = new Intl.PluralRules("en");
        expect(en.resolvedOptions().pluralCategories).toBeDefined();
        expect(en.resolvedOptions().pluralCategories).toEqual([]);
    });
});
