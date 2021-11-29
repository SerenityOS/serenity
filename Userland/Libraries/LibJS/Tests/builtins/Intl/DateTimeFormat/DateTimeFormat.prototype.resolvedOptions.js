// NOTE: We cannot yet test the fields of ECMA-402's Table 4 (week, day, etc.) because those fields
//       won't be copied into the Intl.DateTimeFormat object until the date-time pattern generator
//       actually parses the CLDR patterns (see parse_date_time_pattern).
describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Intl.DateTimeFormat.prototype.resolvedOptions).toHaveLength(0);
    });

    test("locale only contains relevant extension keys", () => {
        const en1 = Intl.NumberFormat("en-u-ca-islamicc");
        expect(en1.resolvedOptions().locale).toBe("en");

        const en2 = Intl.NumberFormat("en-u-nu-latn");
        expect(en2.resolvedOptions().locale).toBe("en-u-nu-latn");

        const en3 = Intl.NumberFormat("en-u-ca-islamicc-nu-latn");
        expect(en3.resolvedOptions().locale).toBe("en-u-nu-latn");
    });

    test("calendar may be set by option", () => {
        const en = Intl.DateTimeFormat("en", { calendar: "gregory" });
        expect(en.resolvedOptions().calendar).toBe("gregory");

        const el = Intl.DateTimeFormat("el", { calendar: "generic" });
        expect(el.resolvedOptions().calendar).toBe("generic");
    });

    test("calendar may be set by locale extension", () => {
        const en = Intl.DateTimeFormat("en-u-ca-gregory");
        expect(en.resolvedOptions().calendar).toBe("gregory");

        const el = Intl.DateTimeFormat("el-u-ca-generic");
        expect(el.resolvedOptions().calendar).toBe("generic");
    });

    test("calendar option overrides locale extension", () => {
        const el = Intl.DateTimeFormat("el-u-ca-generic", { calendar: "gregory" });
        expect(el.resolvedOptions().calendar).toBe("gregory");
    });

    test("calendar option limited to known 'ca' values", () => {
        ["generic", "hello"].forEach(calendar => {
            const en = Intl.DateTimeFormat("en", { calendar: calendar });
            expect(en.resolvedOptions().calendar).toBe("generic");
        });

        ["generic", "hello"].forEach(calendar => {
            const en = Intl.DateTimeFormat(`en-u-ca-${calendar}`);
            expect(en.resolvedOptions().calendar).toBe("generic");
        });
    });

    test("numberingSystem may be set by option", () => {
        const en = Intl.DateTimeFormat("en", { numberingSystem: "latn" });
        expect(en.resolvedOptions().numberingSystem).toBe("latn");

        const el = Intl.DateTimeFormat("el", { numberingSystem: "latn" });
        expect(el.resolvedOptions().numberingSystem).toBe("latn");
    });

    test("numberingSystem may be set by locale extension", () => {
        const en = Intl.DateTimeFormat("en-u-nu-latn");
        expect(en.resolvedOptions().numberingSystem).toBe("latn");

        const el = Intl.DateTimeFormat("el-u-nu-latn");
        expect(el.resolvedOptions().numberingSystem).toBe("latn");
    });

    test("numberingSystem option overrides locale extension", () => {
        const el = Intl.DateTimeFormat("el-u-nu-latn", { numberingSystem: "grek" });
        expect(el.resolvedOptions().numberingSystem).toBe("grek");
    });

    test("numberingSystem option limited to known 'nu' values", () => {
        ["latn", "arab"].forEach(numberingSystem => {
            const en = Intl.DateTimeFormat("en", { numberingSystem: numberingSystem });
            expect(en.resolvedOptions().numberingSystem).toBe("latn");
        });

        ["latn", "arab"].forEach(numberingSystem => {
            const en = Intl.DateTimeFormat(`en-u-nu-${numberingSystem}`);
            expect(en.resolvedOptions().numberingSystem).toBe("latn");
        });

        ["latn", "grek"].forEach(numberingSystem => {
            const el = Intl.DateTimeFormat("el", { numberingSystem: numberingSystem });
            expect(el.resolvedOptions().numberingSystem).toBe(numberingSystem);
        });

        ["latn", "grek"].forEach(numberingSystem => {
            const el = Intl.DateTimeFormat(`el-u-nu-${numberingSystem}`);
            expect(el.resolvedOptions().numberingSystem).toBe(numberingSystem);
        });
    });

    test("style", () => {
        const en = new Intl.DateTimeFormat("en");
        expect(en.resolvedOptions().timeZone).toBe("UTC");

        const el = new Intl.DateTimeFormat("el", { timeZone: "UTC" });
        expect(el.resolvedOptions().timeZone).toBe("UTC");
    });

    test("dateStyle", () => {
        const en = new Intl.DateTimeFormat("en");
        expect(en.resolvedOptions().dateStyle).toBeUndefined();

        ["full", "long", "medium", "short"].forEach(style => {
            const el = new Intl.DateTimeFormat("el", { dateStyle: style });
            expect(el.resolvedOptions().dateStyle).toBe(style);
        });
    });

    test("timeStyle", () => {
        const en = new Intl.DateTimeFormat("en");
        expect(en.resolvedOptions().timeStyle).toBeUndefined();

        ["full", "long", "medium", "short"].forEach(style => {
            const el = new Intl.DateTimeFormat("el", { timeStyle: style });
            expect(el.resolvedOptions().timeStyle).toBe(style);
        });
    });
});
