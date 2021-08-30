describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Intl.DisplayNames.prototype.resolvedOptions).toHaveLength(0);
    });

    test("all valid types", () => {
        const en = new Intl.DisplayNames("en", { type: "region" });
        expect(en.resolvedOptions()).toEqual({
            locale: "en",
            style: "long",
            type: "region",
            fallback: "code",
        });

        const es419 = new Intl.DisplayNames("es-419", { type: "script", fallback: "none" });
        expect(es419.resolvedOptions()).toEqual({
            locale: "es-419",
            style: "long",
            type: "script",
            fallback: "none",
        });

        const zhHant = new Intl.DisplayNames(["zh-Hant"], { type: "language", style: "short" });
        expect(zhHant.resolvedOptions()).toEqual({
            locale: "zh-Hant",
            style: "short",
            type: "language",
            fallback: "code",
        });
    });

    test("locales with extensions", () => {
        const en = new Intl.DisplayNames("en-t-en", { type: "language" });
        expect(en.resolvedOptions().locale).toBe("en");

        const es419 = new Intl.DisplayNames("es-419-u-1k-aaa", { type: "language" });
        expect(es419.resolvedOptions().locale).toBe("es-419");

        const zhHant = new Intl.DisplayNames(["zh-Hant-x-aaa"], { type: "language" });
        expect(zhHant.resolvedOptions().locale).toBe("zh-Hant");
    });
});
