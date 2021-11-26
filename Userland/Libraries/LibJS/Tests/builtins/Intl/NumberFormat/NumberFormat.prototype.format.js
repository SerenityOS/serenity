describe("errors", () => {
    test("called on non-NumberFormat object", () => {
        expect(() => {
            Intl.NumberFormat.prototype.format;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.NumberFormat");

        expect(() => {
            Intl.NumberFormat.prototype.format(1);
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.NumberFormat");
    });

    test("called with value that cannot be converted to a number", () => {
        expect(() => {
            Intl.NumberFormat().format(Symbol.hasInstance);
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to number");
    });

    // FIXME: Remove this and add BigInt tests when BigInt number formatting is supported.
    test("bigint", () => {
        expect(() => {
            Intl.NumberFormat().format(1n);
        }).toThrowWithMessage(
            InternalError,
            "BigInt number formatting is not implemented in LibJS"
        );
    });
});

describe("special values", () => {
    test("NaN", () => {
        const en = new Intl.NumberFormat("en");
        expect(en.format()).toBe("NaN");
        expect(en.format(NaN)).toBe("NaN");
        expect(en.format(undefined)).toBe("NaN");

        const ar = new Intl.NumberFormat("ar");
        expect(ar.format()).toBe("ليس رقم");
        expect(ar.format(NaN)).toBe("ليس رقم");
        expect(ar.format(undefined)).toBe("ليس رقم");
    });

    test("Infinity", () => {
        const en = new Intl.NumberFormat("en");
        expect(en.format(Infinity)).toBe("∞");
        expect(en.format(-Infinity)).toBe("-∞");

        const ar = new Intl.NumberFormat("ar");
        expect(ar.format(Infinity)).toBe("∞");
        expect(ar.format(-Infinity)).toBe("\u061c-∞");
    });
});

describe("style=decimal", () => {
    test("default", () => {
        const en = new Intl.NumberFormat("en");
        expect(en.format(1)).toBe("1");
        expect(en.format(12)).toBe("12");
        expect(en.format(123)).toBe("123");

        const ar = new Intl.NumberFormat("ar");
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(12)).toBe("\u0661\u0662");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663");
    });

    test("integer digits", () => {
        const en = new Intl.NumberFormat("en", { minimumIntegerDigits: 2 });
        expect(en.format(1)).toBe("01");
        expect(en.format(12)).toBe("12");
        expect(en.format(123)).toBe("123");

        const ar = new Intl.NumberFormat("ar", { minimumIntegerDigits: 2 });
        expect(ar.format(1)).toBe("\u0660\u0661");
        expect(ar.format(12)).toBe("\u0661\u0662");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663");
    });

    test("significant digits", () => {
        const en = new Intl.NumberFormat("en", {
            minimumSignificantDigits: 4,
            maximumSignificantDigits: 6,
        });
        expect(en.format(1)).toBe("1.000");
        expect(en.format(12)).toBe("12.00");
        expect(en.format(12.3)).toBe("12.30");
        expect(en.format(12.34)).toBe("12.34");
        expect(en.format(12.345)).toBe("12.345");
        expect(en.format(12.3456)).toBe("12.3456");
        expect(en.format(12.34567)).toBe("12.3457");
        expect(en.format(12.34561)).toBe("12.3456");

        const ar = new Intl.NumberFormat("ar", {
            minimumSignificantDigits: 4,
            maximumSignificantDigits: 6,
        });
        expect(ar.format(1)).toBe("\u0661\u066b\u0660\u0660\u0660");
        expect(ar.format(12)).toBe("\u0661\u0662\u066b\u0660\u0660");
        expect(ar.format(12.3)).toBe("\u0661\u0662\u066b\u0663\u0660");
        expect(ar.format(12.34)).toBe("\u0661\u0662\u066b\u0663\u0664");
        expect(ar.format(12.345)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665");
        expect(ar.format(12.3456)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0666");
        expect(ar.format(12.34567)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0667");
        expect(ar.format(12.34561)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0666");
    });

    test("fraction digits", () => {
        const en = new Intl.NumberFormat("en", {
            minimumFractionDigits: 3,
            maximumFractionDigits: 5,
        });
        expect(en.format(1)).toBe("1.000");
        expect(en.format(12)).toBe("12.000");
        expect(en.format(1.2)).toBe("1.200");
        expect(en.format(1.23)).toBe("1.230");
        expect(en.format(1.234)).toBe("1.234");
        expect(en.format(1.2345)).toBe("1.2345");
        expect(en.format(1.23456)).toBe("1.23456");
        expect(en.format(1.234567)).toBe("1.23457");
        expect(en.format(1.234561)).toBe("1.23456");

        const ar = new Intl.NumberFormat("ar", {
            minimumFractionDigits: 3,
            maximumFractionDigits: 5,
        });
        expect(ar.format(1)).toBe("\u0661\u066b\u0660\u0660\u0660");
        expect(ar.format(12)).toBe("\u0661\u0662\u066b\u0660\u0660\u0660");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662\u0660\u0660");
        expect(ar.format(1.23)).toBe("\u0661\u066b\u0662\u0663\u0660");
        expect(ar.format(1.234)).toBe("\u0661\u066b\u0662\u0663\u0664");
        expect(ar.format(1.2345)).toBe("\u0661\u066b\u0662\u0663\u0664\u0665");
        expect(ar.format(1.23456)).toBe("\u0661\u066b\u0662\u0663\u0664\u0665\u0666");
        expect(ar.format(1.234567)).toBe("\u0661\u066b\u0662\u0663\u0664\u0665\u0667");
        expect(ar.format(1.234561)).toBe("\u0661\u066b\u0662\u0663\u0664\u0665\u0666");
    });

    test("notation=scientific", () => {
        const en = new Intl.NumberFormat("en", { notation: "scientific" });
        expect(en.format(1)).toBe("1E0");
        expect(en.format(1.2)).toBe("1.2E0");
        expect(en.format(12)).toBe("1.2E1");
        expect(en.format(12.3)).toBe("1.23E1");
        expect(en.format(123)).toBe("1.23E2");
        expect(en.format(0.1)).toBe("1E-1");
        expect(en.format(0.12)).toBe("1.2E-1");
        expect(en.format(0.01)).toBe("1E-2");

        const ar = new Intl.NumberFormat("ar", { notation: "scientific" });
        expect(ar.format(1)).toBe("\u0661\u0627\u0633\u0660");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662\u0627\u0633\u0660");
        expect(ar.format(12)).toBe("\u0661\u066b\u0662\u0627\u0633\u0661");
        expect(ar.format(12.3)).toBe("\u0661\u066b\u0662\u0663\u0627\u0633\u0661");
        expect(ar.format(123)).toBe("\u0661\u066b\u0662\u0663\u0627\u0633\u0662");
        expect(ar.format(0.1)).toBe("\u0661\u0627\u0633\u061c-\u0661");
        expect(ar.format(0.12)).toBe("\u0661\u066b\u0662\u0627\u0633\u061c-\u0661");
        expect(ar.format(0.01)).toBe("\u0661\u0627\u0633\u061c-\u0662");
    });

    test("notation=engineering", () => {
        const en = new Intl.NumberFormat("en", { notation: "engineering" });
        expect(en.format(1)).toBe("1E0");
        expect(en.format(1.2)).toBe("1.2E0");
        expect(en.format(12)).toBe("12E0");
        expect(en.format(123)).toBe("123E0");
        expect(en.format(1234)).toBe("1.234E3");
        expect(en.format(12345)).toBe("12.345E3");
        expect(en.format(123456)).toBe("123.456E3");
        expect(en.format(1234567)).toBe("1.235E6");
        expect(en.format(0.1)).toBe("100E-3");
        expect(en.format(0.12)).toBe("120E-3");
        expect(en.format(1.23)).toBe("1.23E0");

        const ar = new Intl.NumberFormat("ar", { notation: "engineering" });
        expect(ar.format(1)).toBe("\u0661\u0627\u0633\u0660");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662\u0627\u0633\u0660");
        expect(ar.format(12)).toBe("\u0661\u0662\u0627\u0633\u0660");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663\u0627\u0633\u0660");
        expect(ar.format(1234)).toBe("\u0661\u066b\u0662\u0663\u0664\u0627\u0633\u0663");
        expect(ar.format(12345)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0627\u0633\u0663");
        expect(ar.format(123456)).toBe(
            "\u0661\u0662\u0663\u066b\u0664\u0665\u0666\u0627\u0633\u0663"
        );
        expect(ar.format(1234567)).toBe("\u0661\u066b\u0662\u0663\u0665\u0627\u0633\u0666");
        expect(ar.format(0.1)).toBe("\u0661\u0660\u0660\u0627\u0633\u061c-\u0663");
        expect(ar.format(0.12)).toBe("\u0661\u0662\u0660\u0627\u0633\u061c-\u0663");
        expect(ar.format(1.23)).toBe("\u0661\u066b\u0662\u0663\u0627\u0633\u0660");
    });

    test("notation=compact", () => {
        const en = new Intl.NumberFormat("en", { notation: "compact" });
        expect(en.format(1)).toBe("1");
        expect(en.format(1.2)).toBe("1.2");
        expect(en.format(1.23)).toBe("1.2");
        expect(en.format(1.29)).toBe("1.3");
        expect(en.format(12)).toBe("12");
        expect(en.format(12.3)).toBe("12");
        expect(en.format(12.34)).toBe("12");

        const ar = new Intl.NumberFormat("ar", { notation: "compact" });
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662");
        expect(ar.format(1.23)).toBe("\u0661\u066b\u0662");
        expect(ar.format(1.29)).toBe("\u0661\u066b\u0663");
        expect(ar.format(12)).toBe("\u0661\u0662");
        expect(ar.format(12.3)).toBe("\u0661\u0662");
        expect(ar.format(12.34)).toBe("\u0661\u0662");
    });

    test("notation=compact, compactDisplay=long", () => {
        const en = new Intl.NumberFormat("en", { notation: "compact", compactDisplay: "long" });
        expect(en.format(1)).toBe("1");
        expect(en.format(1200)).toBe("1.2 thousand");
        expect(en.format(1290)).toBe("1.3 thousand");
        expect(en.format(12000)).toBe("12 thousand");
        expect(en.format(12900)).toBe("13 thousand");
        expect(en.format(1200000)).toBe("1.2 million");
        expect(en.format(1290000)).toBe("1.3 million");
        expect(en.format(12000000)).toBe("12 million");
        expect(en.format(12900000)).toBe("13 million");

        const ar = new Intl.NumberFormat("ar", { notation: "compact", compactDisplay: "long" });
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(1200)).toBe("\u0661\u066b\u0662 ألف");
        expect(ar.format(1290)).toBe("\u0661\u066b\u0663 ألف");
        expect(ar.format(12000)).toBe("\u0661\u0662 ألف");
        expect(ar.format(12900)).toBe("\u0661\u0663 ألف");
        expect(ar.format(1200000)).toBe("\u0661\u066b\u0662 مليون");
        expect(ar.format(1290000)).toBe("\u0661\u066b\u0663 مليون");
        expect(ar.format(12000000)).toBe("\u0661\u0662 مليون");
        expect(ar.format(12900000)).toBe("\u0661\u0663 مليون");

        const ja = new Intl.NumberFormat("ja", { notation: "compact", compactDisplay: "long" });
        expect(ja.format(1)).toBe("1");
        expect(ja.format(1200)).toBe("1200");
        expect(ja.format(1290)).toBe("1290");
        expect(ja.format(12000)).toBe("1.2万");
        expect(ja.format(12900)).toBe("1.3万");
        expect(ja.format(1200000)).toBe("120万");
        expect(ja.format(1290000)).toBe("129万");
        expect(ja.format(12000000)).toBe("1200万");
        expect(ja.format(12900000)).toBe("1290万");
        expect(ja.format(120000000)).toBe("1.2億");
        expect(ja.format(129000000)).toBe("1.3億");
        expect(ja.format(12000000000)).toBe("120億");
        expect(ja.format(12900000000)).toBe("129億");

        const de = new Intl.NumberFormat("de", { notation: "compact", compactDisplay: "long" });
        expect(de.format(1)).toBe("1");
        expect(de.format(1200)).toBe("1,2 Tausend");
        expect(de.format(1290)).toBe("1,3 Tausend");
        expect(de.format(12000)).toBe("12 Tausend");
        expect(de.format(12900)).toBe("13 Tausend");
        expect(de.format(1200000)).toBe("1,2 Millionen");
        expect(de.format(1290000)).toBe("1,3 Millionen");
        expect(de.format(12000000)).toBe("12 Millionen");
        expect(de.format(12900000)).toBe("13 Millionen");
    });

    test("notation=compact, compactDisplay=short", () => {
        const en = new Intl.NumberFormat("en", { notation: "compact", compactDisplay: "short" });
        expect(en.format(1)).toBe("1");
        expect(en.format(1200)).toBe("1.2K");
        expect(en.format(1290)).toBe("1.3K");
        expect(en.format(12000)).toBe("12K");
        expect(en.format(12900)).toBe("13K");
        expect(en.format(1200000)).toBe("1.2M");
        expect(en.format(1290000)).toBe("1.3M");
        expect(en.format(12000000)).toBe("12M");
        expect(en.format(12900000)).toBe("13M");

        const ar = new Intl.NumberFormat("ar", { notation: "compact", compactDisplay: "short" });
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(1200)).toBe("\u0661\u066b\u0662\u00a0ألف");
        expect(ar.format(1290)).toBe("\u0661\u066b\u0663\u00a0ألف");
        expect(ar.format(12000)).toBe("\u0661\u0662\u00a0ألف");
        expect(ar.format(12900)).toBe("\u0661\u0663\u00a0ألف");
        expect(ar.format(1200000)).toBe("\u0661\u066b\u0662\u00a0مليون");
        expect(ar.format(1290000)).toBe("\u0661\u066b\u0663\u00a0مليون");
        expect(ar.format(12000000)).toBe("\u0661\u0662\u00a0مليون");
        expect(ar.format(12900000)).toBe("\u0661\u0663\u00a0مليون");

        const ja = new Intl.NumberFormat("ja", { notation: "compact", compactDisplay: "short" });
        expect(ja.format(1)).toBe("1");
        expect(ja.format(1200)).toBe("1200");
        expect(ja.format(1290)).toBe("1290");
        expect(ja.format(12000)).toBe("1.2万");
        expect(ja.format(12900)).toBe("1.3万");
        expect(ja.format(1200000)).toBe("120万");
        expect(ja.format(1290000)).toBe("129万");
        expect(ja.format(12000000)).toBe("1200万");
        expect(ja.format(12900000)).toBe("1290万");
        expect(ja.format(120000000)).toBe("1.2億");
        expect(ja.format(129000000)).toBe("1.3億");
        expect(ja.format(12000000000)).toBe("120億");
        expect(ja.format(12900000000)).toBe("129億");

        const de = new Intl.NumberFormat("de", { notation: "compact", compactDisplay: "short" });
        expect(de.format(1)).toBe("1");
        expect(de.format(1200)).toBe("1200");
        expect(de.format(1290)).toBe("1290");
        expect(de.format(12000)).toBe("12.000");
        expect(de.format(12900)).toBe("12.900");
        expect(de.format(1200000)).toBe("1,2\u00a0Mio.");
        expect(de.format(1290000)).toBe("1,3\u00a0Mio.");
        expect(de.format(12000000)).toBe("12\u00a0Mio.");
        expect(de.format(12900000)).toBe("13\u00a0Mio.");
    });

    test("signDisplay=never", () => {
        const en = new Intl.NumberFormat("en", { signDisplay: "never" });
        expect(en.format(1)).toBe("1");
        expect(en.format(-1)).toBe("1");

        const ar = new Intl.NumberFormat("ar", { signDisplay: "never" });
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(-1)).toBe("\u0661");
    });

    test("signDisplay=auto", () => {
        const en = new Intl.NumberFormat("en", { signDisplay: "auto" });
        expect(en.format(0)).toBe("0");
        expect(en.format(1)).toBe("1");
        expect(en.format(-0)).toBe("-0");
        expect(en.format(-1)).toBe("-1");

        const ar = new Intl.NumberFormat("ar", { signDisplay: "auto" });
        expect(ar.format(0)).toBe("\u0660");
        expect(ar.format(1)).toBe("\u0661");
        expect(ar.format(-0)).toBe("\u061c-\u0660");
        expect(ar.format(-1)).toBe("\u061c-\u0661");
    });

    test("signDisplay=always", () => {
        const en = new Intl.NumberFormat("en", { signDisplay: "always" });
        expect(en.format(0)).toBe("+0");
        expect(en.format(1)).toBe("+1");
        expect(en.format(-0)).toBe("-0");
        expect(en.format(-1)).toBe("-1");

        const ar = new Intl.NumberFormat("ar", { signDisplay: "always" });
        expect(ar.format(0)).toBe("\u061c+\u0660");
        expect(ar.format(1)).toBe("\u061c+\u0661");
        expect(ar.format(-0)).toBe("\u061c-\u0660");
        expect(ar.format(-1)).toBe("\u061c-\u0661");
    });

    test("signDisplay=exceptZero", () => {
        const en = new Intl.NumberFormat("en", { signDisplay: "exceptZero" });
        expect(en.format(0)).toBe("0");
        expect(en.format(1)).toBe("+1");
        expect(en.format(-0)).toBe("0");
        expect(en.format(-1)).toBe("-1");

        const ar = new Intl.NumberFormat("ar", { signDisplay: "exceptZero" });
        expect(ar.format(0)).toBe("\u0660");
        expect(ar.format(1)).toBe("\u061c+\u0661");
        expect(ar.format(-0)).toBe("\u0660");
        expect(ar.format(-1)).toBe("\u061c-\u0661");
    });

    test("useGrouping=true", () => {
        const en = new Intl.NumberFormat("en", { useGrouping: true });
        expect(en.format(123)).toBe("123");
        expect(en.format(1234)).toBe("1,234");
        expect(en.format(12345)).toBe("12,345");
        expect(en.format(123456)).toBe("123,456");
        expect(en.format(1234567)).toBe("1,234,567");

        const enIn = new Intl.NumberFormat("en-IN", { useGrouping: true });
        expect(enIn.format(123)).toBe("123");
        expect(enIn.format(1234)).toBe("1,234");
        expect(enIn.format(12345)).toBe("12,345");
        expect(enIn.format(123456)).toBe("1,23,456");
        expect(enIn.format(1234567)).toBe("12,34,567");

        const ar = new Intl.NumberFormat("ar", { useGrouping: true });
        expect(ar.format(123)).toBe("\u0661\u0662\u0663");
        expect(ar.format(1234)).toBe("\u0661\u066c\u0662\u0663\u0664");
        expect(ar.format(12345)).toBe("\u0661\u0662\u066c\u0663\u0664\u0665");
        expect(ar.format(123456)).toBe("\u0661\u0662\u0663\u066c\u0664\u0665\u0666");
        expect(ar.format(1234567)).toBe("\u0661\u066c\u0662\u0663\u0664\u066c\u0665\u0666\u0667");
    });

    test("useGrouping=false", () => {
        const en = new Intl.NumberFormat("en", { useGrouping: false });
        expect(en.format(123)).toBe("123");
        expect(en.format(1234)).toBe("1234");
        expect(en.format(12345)).toBe("12345");
        expect(en.format(123456)).toBe("123456");
        expect(en.format(1234567)).toBe("1234567");

        const enIn = new Intl.NumberFormat("en-IN", { useGrouping: false });
        expect(enIn.format(123)).toBe("123");
        expect(enIn.format(1234)).toBe("1234");
        expect(enIn.format(12345)).toBe("12345");
        expect(enIn.format(123456)).toBe("123456");
        expect(enIn.format(1234567)).toBe("1234567");

        const ar = new Intl.NumberFormat("ar", { useGrouping: false });
        expect(ar.format(123)).toBe("\u0661\u0662\u0663");
        expect(ar.format(1234)).toBe("\u0661\u0662\u0663\u0664");
        expect(ar.format(12345)).toBe("\u0661\u0662\u0663\u0664\u0665");
        expect(ar.format(123456)).toBe("\u0661\u0662\u0663\u0664\u0665\u0666");
        expect(ar.format(1234567)).toBe("\u0661\u0662\u0663\u0664\u0665\u0666\u0667");
    });
});

describe("style=percent", () => {
    test("default", () => {
        const en = new Intl.NumberFormat("en", { style: "percent" });
        expect(en.format(1)).toBe("100%");
        expect(en.format(1.2)).toBe("120%");
        expect(en.format(0.234)).toBe("23%");

        const ar = new Intl.NumberFormat("ar", { style: "percent" });
        expect(ar.format(1)).toBe("\u0661\u0660\u0660\u066a\u061c");
        expect(ar.format(1.2)).toBe("\u0661\u0662\u0660\u066a\u061c");
        expect(ar.format(0.234)).toBe("\u0662\u0663\u066a\u061c");
    });

    test("integer digits", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", minimumIntegerDigits: 2 });
        expect(en.format(0.01)).toBe("01%");
        expect(en.format(0.12)).toBe("12%");
        expect(en.format(1.23)).toBe("123%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", minimumIntegerDigits: 2 });
        expect(ar.format(0.01)).toBe("\u0660\u0661\u066a\u061c");
        expect(ar.format(0.12)).toBe("\u0661\u0662\u066a\u061c");
        expect(ar.format(1.23)).toBe("\u0661\u0662\u0663\u066a\u061c");
    });

    test("significant digits", () => {
        const en = new Intl.NumberFormat("en", {
            style: "percent",
            minimumSignificantDigits: 4,
            maximumSignificantDigits: 6,
        });
        expect(en.format(0.1)).toBe("10.00%");
        expect(en.format(1.2)).toBe("120.0%");
        expect(en.format(1.23)).toBe("123.0%");
        expect(en.format(1.234)).toBe("123.4%");
        expect(en.format(1.2345)).toBe("123.45%");
        expect(en.format(1.23456)).toBe("123.456%");
        expect(en.format(1.234567)).toBe("123.457%");
        expect(en.format(1.234561)).toBe("123.456%");

        const ar = new Intl.NumberFormat("ar", {
            style: "percent",
            minimumSignificantDigits: 4,
            maximumSignificantDigits: 6,
        });
        expect(ar.format(0.1)).toBe("\u0661\u0660\u066b\u0660\u0660\u066a\u061c");
        expect(ar.format(1.2)).toBe("\u0661\u0662\u0660\u066b\u0660\u066a\u061c");
        expect(ar.format(1.23)).toBe("\u0661\u0662\u0663\u066b\u0660\u066a\u061c");
        expect(ar.format(1.234)).toBe("\u0661\u0662\u0663\u066b\u0664\u066a\u061c");
        expect(ar.format(1.2345)).toBe("\u0661\u0662\u0663\u066b\u0664\u0665\u066a\u061c");
        expect(ar.format(1.23456)).toBe("\u0661\u0662\u0663\u066b\u0664\u0665\u0666\u066a\u061c");
        expect(ar.format(1.234567)).toBe("\u0661\u0662\u0663\u066b\u0664\u0665\u0667\u066a\u061c");
        expect(ar.format(1.234561)).toBe("\u0661\u0662\u0663\u066b\u0664\u0665\u0666\u066a\u061c");
    });

    test("fraction digits", () => {
        const en = new Intl.NumberFormat("en", {
            style: "percent",
            minimumFractionDigits: 2,
            maximumFractionDigits: 4,
        });
        expect(en.format(0.01)).toBe("1.00%");
        expect(en.format(0.1)).toBe("10.00%");
        expect(en.format(0.12)).toBe("12.00%");
        expect(en.format(0.123)).toBe("12.30%");
        expect(en.format(0.1234)).toBe("12.34%");
        expect(en.format(0.12345)).toBe("12.345%");
        expect(en.format(0.123456)).toBe("12.3456%");
        expect(en.format(0.1234567)).toBe("12.3457%");
        expect(en.format(0.1234561)).toBe("12.3456%");

        const ar = new Intl.NumberFormat("ar", {
            style: "percent",
            minimumFractionDigits: 2,
            maximumFractionDigits: 4,
        });
        expect(ar.format(0.01)).toBe("\u0661\u066b\u0660\u0660\u066a\u061c");
        expect(ar.format(0.1)).toBe("\u0661\u0660\u066b\u0660\u0660\u066a\u061c");
        expect(ar.format(0.12)).toBe("\u0661\u0662\u066b\u0660\u0660\u066a\u061c");
        expect(ar.format(0.123)).toBe("\u0661\u0662\u066b\u0663\u0660\u066a\u061c");
        expect(ar.format(0.1234)).toBe("\u0661\u0662\u066b\u0663\u0664\u066a\u061c");
        expect(ar.format(0.12345)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u066a\u061c");
        expect(ar.format(0.123456)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0666\u066a\u061c");
        expect(ar.format(0.1234567)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0667\u066a\u061c");
        expect(ar.format(0.1234561)).toBe("\u0661\u0662\u066b\u0663\u0664\u0665\u0666\u066a\u061c");
    });

    test("notation=compact", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", notation: "compact" });
        expect(en.format(0.01)).toBe("1%");
        expect(en.format(0.012)).toBe("1.2%");
        expect(en.format(0.0123)).toBe("1.2%");
        expect(en.format(0.0129)).toBe("1.3%");
        expect(en.format(0.12)).toBe("12%");
        expect(en.format(0.123)).toBe("12%");
        expect(en.format(0.1234)).toBe("12%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", notation: "compact" });
        expect(ar.format(0.01)).toBe("\u0661\u066a\u061c");
        expect(ar.format(0.012)).toBe("\u0661\u066b\u0662\u066a\u061c");
        expect(ar.format(0.0123)).toBe("\u0661\u066b\u0662\u066a\u061c");
        expect(ar.format(0.0129)).toBe("\u0661\u066b\u0663\u066a\u061c");
        expect(ar.format(0.12)).toBe("\u0661\u0662\u066a\u061c");
        expect(ar.format(0.123)).toBe("\u0661\u0662\u066a\u061c");
        expect(ar.format(0.1234)).toBe("\u0661\u0662\u066a\u061c");
    });

    test("signDisplay=never", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", signDisplay: "never" });
        expect(en.format(0.01)).toBe("1%");
        expect(en.format(-0.01)).toBe("1%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", signDisplay: "never" });
        expect(ar.format(0.01)).toBe("\u0661\u066a\u061c");
        expect(ar.format(-0.01)).toBe("\u0661\u066a\u061c");
    });

    test("signDisplay=auto", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", signDisplay: "auto" });
        expect(en.format(0.0)).toBe("0%");
        expect(en.format(0.01)).toBe("1%");
        expect(en.format(-0.0)).toBe("-0%");
        expect(en.format(-0.01)).toBe("-1%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", signDisplay: "auto" });
        expect(ar.format(0.0)).toBe("\u0660\u066a\u061c");
        expect(ar.format(0.01)).toBe("\u0661\u066a\u061c");
        expect(ar.format(-0.0)).toBe("\u061c-\u0660\u066a\u061c");
        expect(ar.format(-0.01)).toBe("\u061c-\u0661\u066a\u061c");
    });

    test("signDisplay=always", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", signDisplay: "always" });
        expect(en.format(0.0)).toBe("+0%");
        expect(en.format(0.01)).toBe("+1%");
        expect(en.format(-0.0)).toBe("-0%");
        expect(en.format(-0.01)).toBe("-1%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", signDisplay: "always" });
        expect(ar.format(0.0)).toBe("\u061c+\u0660\u066a\u061c");
        expect(ar.format(0.01)).toBe("\u061c+\u0661\u066a\u061c");
        expect(ar.format(-0.0)).toBe("\u061c-\u0660\u066a\u061c");
        expect(ar.format(-0.01)).toBe("\u061c-\u0661\u066a\u061c");
    });

    test("signDisplay=exceptZero", () => {
        const en = new Intl.NumberFormat("en", { style: "percent", signDisplay: "exceptZero" });
        expect(en.format(0.0)).toBe("0%");
        expect(en.format(0.01)).toBe("+1%");
        expect(en.format(-0.0)).toBe("0%");
        expect(en.format(-0.01)).toBe("-1%");

        const ar = new Intl.NumberFormat("ar", { style: "percent", signDisplay: "exceptZero" });
        expect(ar.format(0.0)).toBe("\u0660\u066a\u061c");
        expect(ar.format(0.01)).toBe("\u061c+\u0661\u066a\u061c");
        expect(ar.format(-0.0)).toBe("\u0660\u066a\u061c");
        expect(ar.format(-0.01)).toBe("\u061c-\u0661\u066a\u061c");
    });
});

describe("style=currency", () => {
    test("currencyDisplay=code", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "code",
        });
        expect(en1.format(1)).toBe("USD\u00a01.00");
        expect(en1.format(1.2)).toBe("USD\u00a01.20");
        expect(en1.format(1.23)).toBe("USD\u00a01.23");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "KHR",
            currencyDisplay: "code",
        });
        expect(en2.format(1)).toBe("KHR\u00a01.00");
        expect(en2.format(1.2)).toBe("KHR\u00a01.20");
        expect(en2.format(1.23)).toBe("KHR\u00a01.23");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "code",
        });
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0USD");
        expect(ar1.format(1.2)).toBe("\u0661\u066b\u0662\u0660\u00a0USD");
        expect(ar1.format(1.23)).toBe("\u0661\u066b\u0662\u0663\u00a0USD");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "code",
            numberingSystem: "latn",
        });
        expect(ar2.format(1)).toBe("USD\u00a01.00");
        expect(ar2.format(1.2)).toBe("USD\u00a01.20");
        expect(ar2.format(1.23)).toBe("USD\u00a01.23");
    });

    test("currencyDisplay=symbol", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "symbol",
        });
        expect(en1.format(1)).toBe("$1.00");
        expect(en1.format(1.2)).toBe("$1.20");
        expect(en1.format(1.23)).toBe("$1.23");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "KHR",
            currencyDisplay: "symbol",
        });
        expect(en2.format(1)).toBe("KHR\u00a01.00");
        expect(en2.format(1.2)).toBe("KHR\u00a01.20");
        expect(en2.format(1.23)).toBe("KHR\u00a01.23");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "symbol",
        });
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(1.2)).toBe("\u0661\u066b\u0662\u0660\u00a0US$");
        expect(ar1.format(1.23)).toBe("\u0661\u066b\u0662\u0663\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "symbol",
            numberingSystem: "latn",
        });
        expect(ar2.format(1)).toBe("US$\u00a01.00");
        expect(ar2.format(1.2)).toBe("US$\u00a01.20");
        expect(ar2.format(1.23)).toBe("US$\u00a01.23");
    });

    test("currencyDisplay=narrowSymbol", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "narrowSymbol",
        });
        expect(en1.format(1)).toBe("$1.00");
        expect(en1.format(1.2)).toBe("$1.20");
        expect(en1.format(1.23)).toBe("$1.23");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "KHR",
            currencyDisplay: "narrowSymbol",
        });
        expect(en2.format(1)).toBe("៛1.00");
        expect(en2.format(1.2)).toBe("៛1.20");
        expect(en2.format(1.23)).toBe("៛1.23");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "narrowSymbol",
        });
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(1.2)).toBe("\u0661\u066b\u0662\u0660\u00a0US$");
        expect(ar1.format(1.23)).toBe("\u0661\u066b\u0662\u0663\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "narrowSymbol",
            numberingSystem: "latn",
        });
        expect(ar2.format(1)).toBe("US$\u00a01.00");
        expect(ar2.format(1.2)).toBe("US$\u00a01.20");
        expect(ar2.format(1.23)).toBe("US$\u00a01.23");
    });

    test("currencyDisplay=name", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "name",
        });
        expect(en1.format(1)).toBe("1.00 US dollars");
        expect(en1.format(1.2)).toBe("1.20 US dollars");
        expect(en1.format(1.23)).toBe("1.23 US dollars");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "KHR",
            currencyDisplay: "name",
        });
        expect(en2.format(1)).toBe("1.00 Cambodian riels");
        expect(en2.format(1.2)).toBe("1.20 Cambodian riels");
        expect(en2.format(1.23)).toBe("1.23 Cambodian riels");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "name",
        });
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660 دولار أمريكي");
        expect(ar1.format(1.2)).toBe("\u0661\u066b\u0662\u0660 دولار أمريكي");
        expect(ar1.format(1.23)).toBe("\u0661\u066b\u0662\u0663 دولار أمريكي");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencyDisplay: "name",
            numberingSystem: "latn",
        });
        expect(ar2.format(1)).toBe("1.00 دولار أمريكي");
        expect(ar2.format(1.2)).toBe("1.20 دولار أمريكي");
        expect(ar2.format(1.23)).toBe("1.23 دولار أمريكي");
    });

    test("signDisplay=never", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            signDisplay: "never",
        });
        expect(en1.format(1)).toBe("$1.00");
        expect(en1.format(-1)).toBe("$1.00");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "never",
        });
        expect(en2.format(1)).toBe("$1.00");
        expect(en2.format(-1)).toBe("$1.00");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            signDisplay: "never",
        });
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "never",
        });
        expect(ar2.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
    });

    test("signDisplay=auto", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            signDisplay: "auto",
        });
        expect(en1.format(0)).toBe("$0.00");
        expect(en1.format(1)).toBe("$1.00");
        expect(en1.format(-0)).toBe("-$0.00");
        expect(en1.format(-1)).toBe("-$1.00");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "auto",
        });
        expect(en2.format(0)).toBe("$0.00");
        expect(en2.format(1)).toBe("$1.00");
        expect(en2.format(-0)).toBe("($0.00)");
        expect(en2.format(-1)).toBe("($1.00)");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            signDisplay: "auto",
        });
        expect(ar1.format(0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-0)).toBe("\u061c-\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "auto",
        });
        expect(ar2.format(0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(1)).toBe("\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-0)).toBe("\u061c-\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");
    });

    test("signDisplay=always", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            signDisplay: "always",
        });
        expect(en1.format(0)).toBe("+$0.00");
        expect(en1.format(1)).toBe("+$1.00");
        expect(en1.format(-0)).toBe("-$0.00");
        expect(en1.format(-1)).toBe("-$1.00");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "always",
        });
        expect(en2.format(0)).toBe("+$0.00");
        expect(en2.format(1)).toBe("+$1.00");
        expect(en2.format(-0)).toBe("($0.00)");
        expect(en2.format(-1)).toBe("($1.00)");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            signDisplay: "always",
        });
        expect(ar1.format(0)).toBe("\u061c+\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(1)).toBe("\u061c+\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-0)).toBe("\u061c-\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "always",
        });
        expect(ar2.format(0)).toBe("\u061c+\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(1)).toBe("\u061c+\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-0)).toBe("\u061c-\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");
    });

    test("signDisplay=exceptZero", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            signDisplay: "exceptZero",
        });
        expect(en1.format(0)).toBe("$0.00");
        expect(en1.format(1)).toBe("+$1.00");
        expect(en1.format(-0)).toBe("$0.00");
        expect(en1.format(-1)).toBe("-$1.00");

        const en2 = new Intl.NumberFormat("en", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "exceptZero",
        });
        expect(en2.format(0)).toBe("$0.00");
        expect(en2.format(1)).toBe("+$1.00");
        expect(en2.format(-0)).toBe("$0.00");
        expect(en2.format(-1)).toBe("($1.00)");

        const ar1 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            signDisplay: "exceptZero",
        });
        expect(ar1.format(0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(1)).toBe("\u061c+\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar1.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");

        const ar2 = new Intl.NumberFormat("ar", {
            style: "currency",
            currency: "USD",
            currencySign: "accounting",
            signDisplay: "exceptZero",
        });
        expect(ar2.format(0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(1)).toBe("\u061c+\u0661\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-0)).toBe("\u0660\u066b\u0660\u0660\u00a0US$");
        expect(ar2.format(-1)).toBe("\u061c-\u0661\u066b\u0660\u0660\u00a0US$");
    });
});

describe("style=unit", () => {
    test("unitDisplay=long", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "gigabit",
            unitDisplay: "long",
        });
        expect(en1.format(1)).toBe("1 gigabit");
        expect(en1.format(1.2)).toBe("1.2 gigabits");
        expect(en1.format(123)).toBe("123 gigabits");

        const en2 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "long",
        });
        expect(en2.format(1)).toBe("1 kilometer per hour");
        expect(en2.format(1.2)).toBe("1.2 kilometers per hour");
        expect(en2.format(123)).toBe("123 kilometers per hour");

        const ar = new Intl.NumberFormat("ar", {
            style: "unit",
            unit: "foot",
            unitDisplay: "long",
        });
        expect(ar.format(1)).toBe("قدم");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662 قدم");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663 قدم");

        const ja = new Intl.NumberFormat("ja", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "long",
        });
        expect(ja.format(1)).toBe("時速 1 キロメートル");
        expect(ja.format(1.2)).toBe("時速 1.2 キロメートル");
        expect(ja.format(123)).toBe("時速 123 キロメートル");
    });

    test("unitDisplay=short", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "gigabit",
            unitDisplay: "short",
        });
        expect(en1.format(1)).toBe("1 Gb");
        expect(en1.format(1.2)).toBe("1.2 Gb");
        expect(en1.format(123)).toBe("123 Gb");

        const en2 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "short",
        });
        expect(en2.format(1)).toBe("1 km/h");
        expect(en2.format(1.2)).toBe("1.2 km/h");
        expect(en2.format(123)).toBe("123 km/h");

        const ar = new Intl.NumberFormat("ar", {
            style: "unit",
            unit: "foot",
            unitDisplay: "short",
        });
        expect(ar.format(1)).toBe("قدم");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662 قدم");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663 قدم");

        const ja = new Intl.NumberFormat("ja", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "short",
        });
        expect(ja.format(1)).toBe("1 km/h");
        expect(ja.format(1.2)).toBe("1.2 km/h");
        expect(ja.format(123)).toBe("123 km/h");
    });

    test("unitDisplay=narrow", () => {
        const en1 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "gigabit",
            unitDisplay: "narrow",
        });
        expect(en1.format(1)).toBe("1Gb");
        expect(en1.format(1.2)).toBe("1.2Gb");
        expect(en1.format(123)).toBe("123Gb");

        const en2 = new Intl.NumberFormat("en", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "narrow",
        });
        expect(en2.format(1)).toBe("1km/h");
        expect(en2.format(1.2)).toBe("1.2km/h");
        expect(en2.format(123)).toBe("123km/h");

        const ar = new Intl.NumberFormat("ar", {
            style: "unit",
            unit: "foot",
            unitDisplay: "narrow",
        });
        expect(ar.format(1)).toBe("قدم");
        expect(ar.format(1.2)).toBe("\u0661\u066b\u0662 قدم");
        expect(ar.format(123)).toBe("\u0661\u0662\u0663 قدمًا");

        const ja = new Intl.NumberFormat("ja", {
            style: "unit",
            unit: "kilometer-per-hour",
            unitDisplay: "narrow",
        });
        expect(ja.format(1)).toBe("1km/h");
        expect(ja.format(1.2)).toBe("1.2km/h");
        expect(ja.format(123)).toBe("123km/h");
    });
});
