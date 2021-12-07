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

        const el = Intl.DateTimeFormat("el", { calendar: "gregory" });
        expect(el.resolvedOptions().calendar).toBe("gregory");
    });

    test("calendar may be set by locale extension", () => {
        const en = Intl.DateTimeFormat("en-u-ca-gregory");
        expect(en.resolvedOptions().calendar).toBe("gregory");

        const el = Intl.DateTimeFormat("el-u-ca-gregory");
        expect(el.resolvedOptions().calendar).toBe("gregory");
    });

    test("calendar option overrides locale extension", () => {
        const el = Intl.DateTimeFormat("el-u-ca-gregory", { calendar: "gregory" });
        expect(el.resolvedOptions().calendar).toBe("gregory");
    });

    test("calendar option limited to known 'ca' values", () => {
        ["gregory", "hello"].forEach(calendar => {
            const en = Intl.DateTimeFormat("en", { calendar: calendar });
            expect(en.resolvedOptions().calendar).toBe("gregory");
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

    test("weekday", () => {
        ["narrow", "short", "long"].forEach(weekday => {
            const en = new Intl.DateTimeFormat("en", { weekday: weekday });
            expect(en.resolvedOptions().weekday).toBe(weekday);
        });
    });

    test("era", () => {
        ["narrow", "short", "long"].forEach(era => {
            const en = new Intl.DateTimeFormat("en", { era: era });
            expect(en.resolvedOptions().era).toBe(era);
        });
    });

    test("year", () => {
        ["2-digit", "numeric"].forEach(year => {
            const en = new Intl.DateTimeFormat("en", { year: year });
            expect(en.resolvedOptions().year).toBe(year);
        });
    });

    test("month", () => {
        ["2-digit", "numeric", "narrow", "short", "long"].forEach(month => {
            const en = new Intl.DateTimeFormat("en", { month: month });
            expect(en.resolvedOptions().month).toBe(month);
        });
    });

    test("day", () => {
        ["2-digit", "numeric"].forEach(day => {
            const en = new Intl.DateTimeFormat("en", { day: day });
            expect(en.resolvedOptions().day).toBe(day);
        });
    });

    test("dayPeriod", () => {
        ["narrow", "short", "long"].forEach(dayPeriod => {
            const en = new Intl.DateTimeFormat("en", { dayPeriod: dayPeriod });
            expect(en.resolvedOptions().dayPeriod).toBe(dayPeriod);
        });
    });

    test("hour", () => {
        ["2-digit", "numeric"].forEach(hour => {
            const en = new Intl.DateTimeFormat("en", { hour: hour });
            expect(en.resolvedOptions().hour).toBe(hour);
        });
    });

    test("minute", () => {
        ["2-digit", "numeric"].forEach(minute => {
            const en = new Intl.DateTimeFormat("en", { minute: minute });
            expect(en.resolvedOptions().minute).toBe("2-digit");
        });
    });

    test("second", () => {
        ["2-digit", "numeric"].forEach(second => {
            const en = new Intl.DateTimeFormat("en", { second: second });
            expect(en.resolvedOptions().second).toBe("2-digit");
        });
    });

    test("fractionalSecondDigits", () => {
        [1, 2, 3].forEach(fractionalSecondDigits => {
            const en = new Intl.DateTimeFormat("en", {
                fractionalSecondDigits: fractionalSecondDigits,
            });
            expect(en.resolvedOptions().fractionalSecondDigits).toBe(fractionalSecondDigits);
        });
    });

    test("timeZoneName", () => {
        ["short", "long"].forEach(timeZoneName => {
            const en = new Intl.DateTimeFormat("en", { timeZoneName: timeZoneName });
            expect(en.resolvedOptions().timeZoneName).toBe(timeZoneName);
        });
    });
});
