describe("errors", () => {
    test("called on non-DateTimeFormat object", () => {
        expect(() => {
            Intl.DateTimeFormat.prototype.format;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.DateTimeFormat");

        expect(() => {
            Intl.DateTimeFormat.prototype.format(1);
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.DateTimeFormat");
    });

    test("called with value that cannot be converted to a number", () => {
        expect(() => {
            Intl.DateTimeFormat().format(Symbol.hasInstance);
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to number");

        expect(() => {
            Intl.DateTimeFormat().format(1n);
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");
    });

    test("time value cannot be clipped", () => {
        expect(() => {
            Intl.DateTimeFormat().format(NaN);
        }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");

        expect(() => {
            Intl.DateTimeFormat().format(-8.65e15);
        }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");

        expect(() => {
            Intl.DateTimeFormat().format(8.65e15);
        }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");
    });
});

const d0 = Date.UTC(2021, 11, 7, 17, 40, 50, 456);
const d1 = Date.UTC(1989, 0, 23, 7, 8, 9, 45);

describe("dateStyle", () => {
    // prettier-ignore
    const data = [
        { date: "full", en0: "Tuesday, December 7, 2021", en1: "Monday, January 23, 1989", ar0: "الثلاثاء، ٧ ديسمبر ٢٠٢١", ar1: "الاثنين، ٢٣ يناير ١٩٨٩" },
        { date: "long", en0: "December 7, 2021", en1: "January 23, 1989", ar0: "٧ ديسمبر ٢٠٢١", ar1: "٢٣ يناير ١٩٨٩" },
        { date: "medium", en0: "Dec 7, 2021", en1: "Jan 23, 1989", ar0: "٠٧‏/١٢‏/٢٠٢١", ar1: "٢٣‏/٠١‏/١٩٨٩" },
        { date: "short", en0: "12/7/21", en1: "1/23/89", ar0: "٧‏/١٢‏/٢٠٢١", ar1: "٢٣‏/١‏/١٩٨٩" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { dateStyle: d.date, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { dateStyle: d.date, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("timeStyle", () => {
    // prettier-ignore
    const data = [
        { time: "full", en0: "5:40:50 PM Coordinated Universal Time", en1: "7:08:09 AM Coordinated Universal Time", ar0: "٥:٤٠:٥٠ م التوقيت العالمي المنسق", ar1: "٧:٠٨:٠٩ ص التوقيت العالمي المنسق" },
        { time: "long", en0: "5:40:50 PM UTC", en1: "7:08:09 AM UTC", ar0: "٥:٤٠:٥٠ م UTC", ar1: "٧:٠٨:٠٩ ص UTC" },
        { time: "medium", en0: "5:40:50 PM", en1: "7:08:09 AM", ar0: "٥:٤٠:٥٠ م", ar1: "٧:٠٨:٠٩ ص" },
        { time: "short", en0: "5:40 PM", en1: "7:08 AM", ar0: "٥:٤٠ م", ar1: "٧:٠٨ ص" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { timeStyle: d.time, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { timeStyle: d.time, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("dateStyle + timeStyle", () => {
    // prettier-ignore
    const data = [
        { date: "full", time: "full", en: "Tuesday, December 7, 2021 at 5:40:50 PM Coordinated Universal Time", ar: "الثلاثاء، ٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م التوقيت العالمي المنسق" },
        { date: "full", time: "long", en: "Tuesday, December 7, 2021 at 5:40:50 PM UTC", ar: "الثلاثاء، ٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م UTC" },
        { date: "full", time: "medium", en: "Tuesday, December 7, 2021 at 5:40:50 PM", ar: "الثلاثاء، ٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م" },
        { date: "full", time: "short", en: "Tuesday, December 7, 2021 at 5:40 PM", ar: "الثلاثاء، ٧ ديسمبر ٢٠٢١ في ٥:٤٠ م" },
        { date: "long", time: "full", en: "December 7, 2021 at 5:40:50 PM Coordinated Universal Time", ar: "٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م التوقيت العالمي المنسق" },
        { date: "long", time: "long", en: "December 7, 2021 at 5:40:50 PM UTC", ar: "٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م UTC" },
        { date: "long", time: "medium", en: "December 7, 2021 at 5:40:50 PM", ar: "٧ ديسمبر ٢٠٢١ في ٥:٤٠:٥٠ م" },
        { date: "long", time: "short", en: "December 7, 2021 at 5:40 PM", ar: "٧ ديسمبر ٢٠٢١ في ٥:٤٠ م" },
        { date: "medium", time: "full", en: "Dec 7, 2021, 5:40:50 PM Coordinated Universal Time", ar: "٠٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م التوقيت العالمي المنسق" },
        { date: "medium", time: "long", en: "Dec 7, 2021, 5:40:50 PM UTC", ar: "٠٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م UTC" },
        { date: "medium", time: "medium", en: "Dec 7, 2021, 5:40:50 PM", ar: "٠٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م" },
        { date: "medium", time: "short", en: "Dec 7, 2021, 5:40 PM", ar: "٠٧‏/١٢‏/٢٠٢١, ٥:٤٠ م" },
        { date: "short", time: "full", en: "12/7/21, 5:40:50 PM Coordinated Universal Time", ar: "٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م التوقيت العالمي المنسق" },
        { date: "short", time: "long", en: "12/7/21, 5:40:50 PM UTC", ar: "٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م UTC" },
        { date: "short", time: "medium", en: "12/7/21, 5:40:50 PM", ar: "٧‏/١٢‏/٢٠٢١, ٥:٤٠:٥٠ م" },
        { date: "short", time: "short", en: "12/7/21, 5:40 PM", ar: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                dateStyle: d.date,
                timeStyle: d.time,
                timeZone: "UTC",
            });
            expect(en.format(d0)).toBe(d.en);

            const ar = new Intl.DateTimeFormat("ar", {
                dateStyle: d.date,
                timeStyle: d.time,
                timeZone: "UTC",
            });
            expect(ar.format(d0)).toBe(d.ar);
        });
    });
});

describe("weekday", () => {
    // prettier-ignore
    const data = [
        { weekday: "narrow", en0: "T", en1: "M", ar0: "ث", ar1: "ن" },
        { weekday: "short", en0: "Tue", en1: "Mon", ar0: "الثلاثاء", ar1: "الاثنين" },
        { weekday: "long", en0: "Tuesday", en1: "Monday", ar0: "الثلاثاء", ar1: "الاثنين" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { weekday: d.weekday, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { weekday: d.weekday, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("era", () => {
    // prettier-ignore
    const data = [
        { era: "narrow", en0: "12/7/2021 A", en1: "1/23/1989 A", ar0: "٧ ١٢ ٢٠٢١ م", ar1: "٢٣ ١ ١٩٨٩ م" },
        { era: "short", en0: "12/7/2021 AD", en1: "1/23/1989 AD", ar0: "٧ ١٢ ٢٠٢١ م", ar1: "٢٣ ١ ١٩٨٩ م" },
        { era: "long", en0: "12/7/2021 Anno Domini", en1: "1/23/1989 Anno Domini", ar0: "٧ ١٢ ٢٠٢١ ميلادي", ar1: "٢٣ ١ ١٩٨٩ ميلادي" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { era: d.era, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { era: d.era, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("year", () => {
    // prettier-ignore
    const data = [
        { year: "2-digit", en0: "21", en1: "89", ar0: "٢١", ar1: "٨٩" },
        { year: "numeric", en0: "2021", en1: "1989", ar0: "٢٠٢١", ar1: "١٩٨٩" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { year: d.year, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { year: d.year, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("month", () => {
    // prettier-ignore
    const data = [
        { month: "2-digit", en0: "12", en1: "01", ar0: "١٢", ar1: "٠١" },
        { month: "numeric", en0: "12", en1: "1", ar0: "١٢", ar1: "١" },
        { month: "narrow", en0: "D", en1: "J", ar0: "د", ar1: "ي" },
        { month: "short", en0: "Dec", en1: "Jan", ar0: "ديسمبر", ar1: "يناير" },
        { month: "long", en0: "December", en1: "January", ar0: "ديسمبر", ar1: "يناير" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { month: d.month, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { month: d.month, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("day", () => {
    // prettier-ignore
    const data = [
        { day: "2-digit", en0: "07", en1: "23", ar0: "٠٧", ar1: "٢٣" },
        { day: "numeric", en0: "7", en1: "23", ar0: "٧", ar1: "٢٣" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { day: d.day, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { day: d.day, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("dayPeriod", () => {
    // prettier-ignore
    const data = [
        { dayPeriod: "narrow", en0: "5 in the afternoon", en1: "7 in the morning", ar0: "٥ بعد الظهر", ar1: "٧ صباحًا", as0: "অপৰাহ্ন ৫", as1: "পূৰ্বাহ্ন ৭"},
        { dayPeriod: "short", en0: "5 in the afternoon", en1: "7 in the morning", ar0: "٥ بعد الظهر", ar1: "٧ ص", as0: "অপৰাহ্ন ৫", as1: "পূৰ্বাহ্ন ৭"},
        { dayPeriod: "long", en0: "5 in the afternoon", en1: "7 in the morning", ar0: "٥ بعد الظهر", ar1: "٧ صباحًا", as0: "অপৰাহ্ন ৫", as1: "পূৰ্বাহ্ন ৭"},
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                dayPeriod: d.dayPeriod,
                hour: "numeric",
                timeZone: "UTC",
            });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", {
                dayPeriod: d.dayPeriod,
                hour: "numeric",
                timeZone: "UTC",
            });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);

            const as = new Intl.DateTimeFormat("as", {
                dayPeriod: d.dayPeriod,
                hour: "numeric",
                timeZone: "UTC",
            });
            expect(as.format(d0)).toBe(d.as0);
            expect(as.format(d1)).toBe(d.as1);
        });
    });

    test("flexible day period rolls over midnight", () => {
        // For the en locale, this time (05:00) falls in the flexible day period range of [21:00, 06:00).
        const date = Date.UTC(2017, 11, 12, 5, 0, 0, 0);

        const en = new Intl.DateTimeFormat("en", { dayPeriod: "short", timeZone: "UTC" });
        expect(en.format(date)).toBe("5 at night");
    });
});

describe("hour", () => {
    // prettier-ignore
    // FIXME: The 2-digit results are supposed to include {ampm}. These results are achieved from the "HH"
    //        pattern, which should only be applied to 24-hour cycles.
    const data = [
        { hour: "2-digit", en0: "05", en1: "07", ar0: "٠٥", ar1: "٠٧" },
        { hour: "numeric", en0: "5 PM", en1: "7 AM", ar0: "٥ م", ar1: "٧ ص" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", { hour: d.hour, timeZone: "UTC" });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", { hour: d.hour, timeZone: "UTC" });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("minute", () => {
    // prettier-ignore
    const data = [
        { minute: "2-digit", en0: "5:40 PM", en1: "7:08 AM", ar0: "٥:٤٠ م", ar1: "٧:٠٨ ص" },
        { minute: "numeric", en0: "5:40 PM", en1: "7:08 AM", ar0: "٥:٤٠ م", ar1: "٧:٠٨ ص" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                minute: d.minute,
                hour: "numeric",
                timeZone: "UTC",
            });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", {
                minute: d.minute,
                hour: "numeric",
                timeZone: "UTC",
            });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("second", () => {
    // prettier-ignore
    const data = [
        { second: "2-digit", en0: "40:50", en1: "08:09", ar0: "٤٠:٥٠", ar1: "٠٨:٠٩" },
        { second: "numeric", en0: "40:50", en1: "08:09", ar0: "٤٠:٥٠", ar1: "٠٨:٠٩" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                second: d.second,
                minute: "numeric",
                timeZone: "UTC",
            });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", {
                second: d.second,
                minute: "numeric",
                timeZone: "UTC",
            });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("fractionalSecondDigits", () => {
    // prettier-ignore
    const data = [
        { fractionalSecondDigits: 1, en0: "40:50.4", en1: "08:09.0", ar0: "٤٠:٥٠٫٤", ar1: "٠٨:٠٩٫٠" },
        { fractionalSecondDigits: 2, en0: "40:50.45", en1: "08:09.04", ar0: "٤٠:٥٠٫٤٥", ar1: "٠٨:٠٩٫٠٤" },
        { fractionalSecondDigits: 3, en0: "40:50.456", en1: "08:09.045", ar0: "٤٠:٥٠٫٤٥٦", ar1: "٠٨:٠٩٫٠٤٥" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                fractionalSecondDigits: d.fractionalSecondDigits,
                second: "numeric",
                minute: "numeric",
                timeZone: "UTC",
            });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", {
                fractionalSecondDigits: d.fractionalSecondDigits,
                second: "numeric",
                minute: "numeric",
                timeZone: "UTC",
            });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});

describe("timeZoneName", () => {
    // prettier-ignore
    const data = [
        { timeZone: "UTC", timeZoneName: "short", en0: "12/7/2021, 5:40 PM UTC", en1: "1/23/1989, 7:08 AM UTC", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م UTC", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص UTC" },
        { timeZone: "UTC", timeZoneName: "long", en0: "12/7/2021, 5:40 PM Coordinated Universal Time", en1: "1/23/1989, 7:08 AM Coordinated Universal Time", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م التوقيت العالمي المنسق", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص التوقيت العالمي المنسق" },
        { timeZone: "UTC", timeZoneName: "shortOffset", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "UTC", timeZoneName: "longOffset", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "UTC", timeZoneName: "shortGeneric", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "UTC", timeZoneName: "longGeneric", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },

        { timeZone: "America/New_York", timeZoneName: "short", en0: "12/7/2021, 12:40 PM EST", en1: "1/23/1989, 2:08 AM EST", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م غرينتش-٥", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص غرينتش-٥" },
        { timeZone: "America/New_York", timeZoneName: "long", en0: "12/7/2021, 12:40 PM Eastern Standard Time", en1: "1/23/1989, 2:08 AM Eastern Standard Time", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م التوقيت الرسمي الشرقي لأمريكا الشمالية", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص التوقيت الرسمي الشرقي لأمريكا الشمالية" },
        { timeZone: "America/New_York", timeZoneName: "shortOffset", en0: "12/7/2021, 12:40 PM GMT-5", en1: "1/23/1989, 2:08 AM GMT-5", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م غرينتش-٥", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص غرينتش-٥" },
        { timeZone: "America/New_York", timeZoneName: "longOffset", en0: "12/7/2021, 12:40 PM GMT-05:00", en1: "1/23/1989, 2:08 AM GMT-05:00", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م غرينتش-٠٥:٠٠", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص غرينتش-٠٥:٠٠" },
        { timeZone: "America/New_York", timeZoneName: "shortGeneric", en0: "12/7/2021, 12:40 PM ET", en1: "1/23/1989, 2:08 AM ET", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م غرينتش-٥", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص غرينتش-٥" },
        { timeZone: "America/New_York", timeZoneName: "longGeneric", en0: "12/7/2021, 12:40 PM Eastern Time", en1: "1/23/1989, 2:08 AM Eastern Time", ar0: "٧‏/١٢‏/٢٠٢١, ١٢:٤٠ م التوقيت الشرقي لأمريكا الشمالية", ar1: "٢٣‏/١‏/١٩٨٩, ٢:٠٨ ص التوقيت الشرقي لأمريكا الشمالية" },

        { timeZone: "Europe/London", timeZoneName: "short", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "Europe/London", timeZoneName: "long", en0: "12/7/2021, 5:40 PM Greenwich Mean Time", en1: "1/23/1989, 7:08 AM Greenwich Mean Time", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م توقيت غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص توقيت غرينتش" },
        { timeZone: "Europe/London", timeZoneName: "shortOffset", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "Europe/London", timeZoneName: "longOffset", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "Europe/London", timeZoneName: "shortGeneric", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },
        { timeZone: "Europe/London", timeZoneName: "longGeneric", en0: "12/7/2021, 5:40 PM GMT", en1: "1/23/1989, 7:08 AM GMT", ar0: "٧‏/١٢‏/٢٠٢١, ٥:٤٠ م غرينتش", ar1: "٢٣‏/١‏/١٩٨٩, ٧:٠٨ ص غرينتش" },

        { timeZone: "America/Los_Angeles", timeZoneName: "short", en0: "12/7/2021, 9:40 AM PST", en1: "1/22/1989, 11:08 PM PST", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص غرينتش-٨", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م غرينتش-٨" },
        { timeZone: "America/Los_Angeles", timeZoneName: "long", en0: "12/7/2021, 9:40 AM Pacific Standard Time", en1: "1/22/1989, 11:08 PM Pacific Standard Time", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص توقيت المحيط الهادي الرسمي", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م توقيت المحيط الهادي الرسمي" },
        { timeZone: "America/Los_Angeles", timeZoneName: "shortOffset", en0: "12/7/2021, 9:40 AM GMT-8", en1: "1/22/1989, 11:08 PM GMT-8", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص غرينتش-٨", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م غرينتش-٨" },
        { timeZone: "America/Los_Angeles", timeZoneName: "longOffset", en0: "12/7/2021, 9:40 AM GMT-08:00", en1: "1/22/1989, 11:08 PM GMT-08:00", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص غرينتش-٠٨:٠٠", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م غرينتش-٠٨:٠٠" },
        { timeZone: "America/Los_Angeles", timeZoneName: "shortGeneric", en0: "12/7/2021, 9:40 AM PT", en1: "1/22/1989, 11:08 PM PT", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص غرينتش-٨", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م غرينتش-٨" },
        { timeZone: "America/Los_Angeles", timeZoneName: "longGeneric", en0: "12/7/2021, 9:40 AM Pacific Time", en1: "1/22/1989, 11:08 PM Pacific Time", ar0: "٧‏/١٢‏/٢٠٢١, ٩:٤٠ ص توقيت المحيط الهادي", ar1: "٢٢‏/١‏/١٩٨٩, ١١:٠٨ م توقيت المحيط الهادي" },

        { timeZone: "Asia/Kathmandu", timeZoneName: "short", en0: "12/7/2021, 11:25 PM GMT+5:45", en1: "1/23/1989, 12:53 PM GMT+5:45", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م غرينتش+٥:٤٥", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م غرينتش+٥:٤٥" },
        { timeZone: "Asia/Kathmandu", timeZoneName: "long", en0: "12/7/2021, 11:25 PM Nepal Time", en1: "1/23/1989, 12:53 PM Nepal Time", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م توقيت نيبال", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م توقيت نيبال" },
        { timeZone: "Asia/Kathmandu", timeZoneName: "shortOffset", en0: "12/7/2021, 11:25 PM GMT+5:45", en1: "1/23/1989, 12:53 PM GMT+5:45", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م غرينتش+٥:٤٥", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م غرينتش+٥:٤٥" },
        { timeZone: "Asia/Kathmandu", timeZoneName: "longOffset", en0: "12/7/2021, 11:25 PM GMT+05:45", en1: "1/23/1989, 12:53 PM GMT+05:45", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م غرينتش+٠٥:٤٥", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م غرينتش+٠٥:٤٥" },
        { timeZone: "Asia/Kathmandu", timeZoneName: "shortGeneric", en0: "12/7/2021, 11:25 PM GMT+5:45", en1: "1/23/1989, 12:53 PM GMT+5:45", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م غرينتش+٥:٤٥", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م غرينتش+٥:٤٥" },
        { timeZone: "Asia/Kathmandu", timeZoneName: "longGeneric", en0: "12/7/2021, 11:25 PM GMT+05:45", en1: "1/23/1989, 12:53 PM GMT+05:45", ar0: "٧‏/١٢‏/٢٠٢١, ١١:٢٥ م غرينتش+٠٥:٤٥", ar1: "٢٣‏/١‏/١٩٨٩, ١٢:٥٣ م غرينتش+٠٥:٤٥" },
    ];

    test("all", () => {
        data.forEach(d => {
            const en = new Intl.DateTimeFormat("en", {
                timeZone: d.timeZone,
                timeZoneName: d.timeZoneName,
            });
            expect(en.format(d0)).toBe(d.en0);
            expect(en.format(d1)).toBe(d.en1);

            const ar = new Intl.DateTimeFormat("ar", {
                timeZone: d.timeZone,
                timeZoneName: d.timeZoneName,
            });
            expect(ar.format(d0)).toBe(d.ar0);
            expect(ar.format(d1)).toBe(d.ar1);
        });
    });
});
