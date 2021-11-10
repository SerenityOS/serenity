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
