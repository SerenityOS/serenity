describe("errors", () => {
    test("invalid language", () => {
        expect(() => {
            new Intl.DisplayNames("en", { type: "language" }).of("hello!");
        }).toThrowWithMessage(RangeError, "'hello!' is not a valid value for option type language");
    });

    test("invalid region", () => {
        expect(() => {
            new Intl.DisplayNames("en", { type: "region" }).of("hello!");
        }).toThrowWithMessage(RangeError, "'hello!' is not a valid value for option type region");
    });

    test("invalid script", () => {
        expect(() => {
            new Intl.DisplayNames("en", { type: "script" }).of("hello!");
        }).toThrowWithMessage(RangeError, "'hello!' is not a valid value for option type script");
    });

    test("invalid currency", () => {
        expect(() => {
            new Intl.DisplayNames("en", { type: "currency" }).of("hello!");
        }).toThrowWithMessage(RangeError, "'hello!' is not a valid value for option type currency");
    });
});

describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Intl.DisplayNames.prototype.of).toHaveLength(1);
    });

    test("option type language", () => {
        const en = new Intl.DisplayNames("en", { type: "language" });
        expect(en.of("en")).toBe("English");

        const es419 = new Intl.DisplayNames("es-419", { type: "language" });
        expect(es419.of("en")).toBe("inglés");

        const zhHant = new Intl.DisplayNames(["zh-Hant"], { type: "language" });
        expect(zhHant.of("en")).toBe("英文");

        expect(en.of("zz")).toBe("zz");
        expect(es419.of("zz")).toBe("zz");
        expect(zhHant.of("zz")).toBe("zz");
    });

    test("option type region", () => {
        const en = new Intl.DisplayNames("en", { type: "region" });
        expect(en.of("US")).toBe("United States");

        const es419 = new Intl.DisplayNames("es-419", { type: "region" });
        expect(es419.of("US")).toBe("Estados Unidos");

        const zhHant = new Intl.DisplayNames(["zh-Hant"], { type: "region" });
        expect(zhHant.of("US")).toBe("美國");

        expect(en.of("AA")).toBe("AA");
        expect(es419.of("AA")).toBe("AA");
        expect(zhHant.of("AA")).toBe("AA");
    });

    test("option type script", () => {
        const en = new Intl.DisplayNames("en", { type: "script" });
        expect(en.of("Latn")).toBe("Latin");

        const es419 = new Intl.DisplayNames("es-419", { type: "script" });
        expect(es419.of("Latn")).toBe("latín");

        const zhHant = new Intl.DisplayNames(["zh-Hant"], { type: "script" });
        expect(zhHant.of("Latn")).toBe("拉丁文");

        expect(en.of("Aaaa")).toBe("Aaaa");
        expect(es419.of("Aaaa")).toBe("Aaaa");
        expect(zhHant.of("Aaaa")).toBe("Aaaa");
    });
});
