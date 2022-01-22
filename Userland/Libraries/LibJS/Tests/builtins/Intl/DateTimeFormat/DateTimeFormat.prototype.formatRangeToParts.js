describe("errors", () => {
    test("called on non-DateTimeFormat object", () => {
        expect(() => {
            Intl.DateTimeFormat.prototype.formatRangeToParts(1, 2);
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.DateTimeFormat");
    });

    test("called with undefined values", () => {
        expect(() => {
            Intl.DateTimeFormat().formatRangeToParts();
        }).toThrowWithMessage(TypeError, "startDate is undefined");

        expect(() => {
            Intl.DateTimeFormat().formatRangeToParts(1);
        }).toThrowWithMessage(TypeError, "endDate is undefined");
    });

    test("called with values that cannot be converted to numbers", () => {
        expect(() => {
            Intl.DateTimeFormat().formatRangeToParts(1, Symbol.hasInstance);
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to number");

        expect(() => {
            Intl.DateTimeFormat().formatRangeToParts(1n, 1);
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");
    });

    test("time value cannot be clipped", () => {
        [NaN, -8.65e15, 8.65e15].forEach(d => {
            expect(() => {
                Intl.DateTimeFormat().formatRangeToParts(d, 1);
            }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");

            expect(() => {
                Intl.DateTimeFormat().formatRangeToParts(1, d);
            }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");
        });
    });

    test("called with values in bad order", () => {
        expect(() => {
            Intl.DateTimeFormat().formatRangeToParts(new Date(2021), new Date(1989));
        }).toThrowWithMessage(RangeError, "Start time 2021 is after end time 1989");
    });
});

const d0 = Date.UTC(1989, 0, 23, 7, 8, 9, 45);
const d1 = Date.UTC(2021, 11, 7, 17, 40, 50, 456);

describe("equal dates are squashed", () => {
    test("with date fields", () => {
        const en = new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "long",
            day: "2-digit",
            timeZone: "UTC",
        });
        expect(en.formatRangeToParts(d0, d0)).toEqual([
            { type: "month", value: "January", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "day", value: "23", source: "shared" },
            { type: "literal", value: ", ", source: "shared" },
            { type: "year", value: "1989", source: "shared" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", {
            year: "numeric",
            month: "long",
            day: "2-digit",
            timeZone: "UTC",
        });
        expect(ja.formatRangeToParts(d0, d0)).toEqual([
            { type: "year", value: "1989", source: "shared" },
            { type: "literal", value: "/", source: "shared" },
            { type: "month", value: "1月", source: "shared" },
            { type: "literal", value: "/", source: "shared" },
            { type: "day", value: "23", source: "shared" },
        ]);
    });

    test("with time fields", () => {
        const en = new Intl.DateTimeFormat("en", {
            hour: "numeric",
            minute: "2-digit",
            second: "2-digit",
            timeZone: "UTC",
        });
        expect(en.formatRangeToParts(d0, d0)).toEqual([
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "dayPeriod", value: "AM", source: "shared" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", {
            hour: "numeric",
            minute: "2-digit",
            second: "2-digit",
            timeZone: "UTC",
        });
        expect(ja.formatRangeToParts(d0, d0)).toEqual([
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
        ]);
    });

    test("with mixed fields", () => {
        const en = new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "long",
            day: "2-digit",
            hour: "numeric",
            minute: "2-digit",
            second: "2-digit",
            timeZone: "UTC",
        });
        expect(en.formatRangeToParts(d0, d0)).toEqual([
            { type: "month", value: "January", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "day", value: "23", source: "shared" },
            { type: "literal", value: ", ", source: "shared" },
            { type: "year", value: "1989", source: "shared" },
            { type: "literal", value: " at ", source: "shared" },
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "dayPeriod", value: "AM", source: "shared" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", {
            year: "numeric",
            month: "long",
            day: "2-digit",
            hour: "numeric",
            minute: "2-digit",
            second: "2-digit",
            timeZone: "UTC",
        });
        expect(ja.formatRangeToParts(d0, d0)).toEqual([
            { type: "year", value: "1989", source: "shared" },
            { type: "literal", value: "/", source: "shared" },
            { type: "month", value: "1月", source: "shared" },
            { type: "literal", value: "/", source: "shared" },
            { type: "day", value: "23", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
        ]);
    });

    test("with date/time style fields", () => {
        const en = new Intl.DateTimeFormat("en", {
            dateStyle: "full",
            timeStyle: "medium",
            timeZone: "UTC",
        });
        expect(en.formatRangeToParts(d0, d0)).toEqual([
            { type: "weekday", value: "Monday", source: "shared" },
            { type: "literal", value: ", ", source: "shared" },
            { type: "month", value: "January", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "day", value: "23", source: "shared" },
            { type: "literal", value: ", ", source: "shared" },
            { type: "year", value: "1989", source: "shared" },
            { type: "literal", value: " at ", source: "shared" },
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "dayPeriod", value: "AM", source: "shared" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", {
            dateStyle: "full",
            timeStyle: "medium",
            timeZone: "UTC",
        });
        expect(ja.formatRangeToParts(d0, d0)).toEqual([
            { type: "year", value: "1989", source: "shared" },
            { type: "literal", value: "年", source: "shared" },
            { type: "month", value: "1", source: "shared" },
            { type: "literal", value: "月", source: "shared" },
            { type: "day", value: "23", source: "shared" },
            { type: "literal", value: "日", source: "shared" },
            { type: "weekday", value: "月曜日", source: "shared" },
            { type: "literal", value: " ", source: "shared" },
            { type: "hour", value: "7", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "minute", value: "08", source: "shared" },
            { type: "literal", value: ":", source: "shared" },
            { type: "second", value: "09", source: "shared" },
        ]);
    });
});

describe("dateStyle", () => {
    test("full", () => {
        const en = new Intl.DateTimeFormat("en", { dateStyle: "full", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "weekday", value: "Monday", source: "startRange" },
            { type: "literal", value: ", ", source: "startRange" },
            { type: "month", value: "January", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: ", ", source: "startRange" },
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "weekday", value: "Tuesday", source: "endRange" },
            { type: "literal", value: ", ", source: "endRange" },
            { type: "month", value: "December", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "day", value: "7", source: "endRange" },
            { type: "literal", value: ", ", source: "endRange" },
            { type: "year", value: "2021", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { dateStyle: "full", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: "年", source: "startRange" },
            { type: "month", value: "1", source: "startRange" },
            { type: "literal", value: "月", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: "日", source: "startRange" },
            { type: "weekday", value: "月曜日", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "year", value: "2021", source: "endRange" },
            { type: "literal", value: "年", source: "endRange" },
            { type: "month", value: "12", source: "endRange" },
            { type: "literal", value: "月", source: "endRange" },
            { type: "day", value: "7", source: "endRange" },
            { type: "literal", value: "日", source: "endRange" },
            { type: "weekday", value: "火曜日", source: "endRange" },
        ]);
    });

    test("long", () => {
        const en = new Intl.DateTimeFormat("en", { dateStyle: "long", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "month", value: "January", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: ", ", source: "startRange" },
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "month", value: "December", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "day", value: "7", source: "endRange" },
            { type: "literal", value: ", ", source: "endRange" },
            { type: "year", value: "2021", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { dateStyle: "long", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "month", value: "01", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "year", value: "2021", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "month", value: "12", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "day", value: "07", source: "endRange" },
        ]);
    });

    test("medium", () => {
        const en = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "month", value: "Jan", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: ", ", source: "startRange" },
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "month", value: "Dec", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "day", value: "7", source: "endRange" },
            { type: "literal", value: ", ", source: "endRange" },
            { type: "year", value: "2021", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { dateStyle: "medium", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "month", value: "01", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "year", value: "2021", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "month", value: "12", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "day", value: "07", source: "endRange" },
        ]);
    });

    test("short", () => {
        const en = new Intl.DateTimeFormat("en", { dateStyle: "short", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "month", value: "1", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "year", value: "89", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "month", value: "12", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "day", value: "7", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "year", value: "21", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { dateStyle: "short", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "year", value: "1989", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "month", value: "01", source: "startRange" },
            { type: "literal", value: "/", source: "startRange" },
            { type: "day", value: "23", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "year", value: "2021", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "month", value: "12", source: "endRange" },
            { type: "literal", value: "/", source: "endRange" },
            { type: "day", value: "07", source: "endRange" },
        ]);
    });
});

describe("timeStyle", () => {
    // FIXME: These results should include the date, even though it isn't requested, because the start/end dates
    //        are more than just hours apart. See the FIXME in PartitionDateTimeRangePattern.
    test("full", () => {
        const en = new Intl.DateTimeFormat("en", { timeStyle: "full", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "dayPeriod", value: "AM", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "timeZoneName", value: "Coordinated Universal Time", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "hour", value: "5", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "dayPeriod", value: "PM", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "timeZoneName", value: "Coordinated Universal Time", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { timeStyle: "full", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: "時", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: "分", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: "秒 ", source: "startRange" },
            { type: "timeZoneName", value: "協定世界時", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "hour", value: "17", source: "endRange" },
            { type: "literal", value: "時", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: "分", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
            { type: "literal", value: "秒 ", source: "endRange" },
            { type: "timeZoneName", value: "協定世界時", source: "endRange" },
        ]);
    });

    test("long", () => {
        const en = new Intl.DateTimeFormat("en", { timeStyle: "long", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "dayPeriod", value: "AM", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "timeZoneName", value: "UTC", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "hour", value: "5", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "dayPeriod", value: "PM", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "timeZoneName", value: "UTC", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { timeStyle: "long", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "timeZoneName", value: "UTC", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "hour", value: "17", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "timeZoneName", value: "UTC", source: "endRange" },
        ]);
    });

    test("medium", () => {
        const en = new Intl.DateTimeFormat("en", { timeStyle: "medium", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "dayPeriod", value: "AM", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "hour", value: "5", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "dayPeriod", value: "PM", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { timeStyle: "medium", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "second", value: "09", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "hour", value: "17", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "second", value: "50", source: "endRange" },
        ]);
    });

    test("short", () => {
        const en = new Intl.DateTimeFormat("en", { timeStyle: "short", timeZone: "UTC" });
        expect(en.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: " ", source: "startRange" },
            { type: "dayPeriod", value: "AM", source: "startRange" },
            { type: "literal", value: " – ", source: "shared" },
            { type: "hour", value: "5", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
            { type: "literal", value: " ", source: "endRange" },
            { type: "dayPeriod", value: "PM", source: "endRange" },
        ]);

        const ja = new Intl.DateTimeFormat("ja", { timeStyle: "short", timeZone: "UTC" });
        expect(ja.formatRangeToParts(d0, d1)).toEqual([
            { type: "hour", value: "7", source: "startRange" },
            { type: "literal", value: ":", source: "startRange" },
            { type: "minute", value: "08", source: "startRange" },
            { type: "literal", value: "～", source: "shared" },
            { type: "hour", value: "17", source: "endRange" },
            { type: "literal", value: ":", source: "endRange" },
            { type: "minute", value: "40", source: "endRange" },
        ]);
    });
});
