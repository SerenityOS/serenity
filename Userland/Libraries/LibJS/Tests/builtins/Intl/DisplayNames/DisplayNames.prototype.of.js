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
});
