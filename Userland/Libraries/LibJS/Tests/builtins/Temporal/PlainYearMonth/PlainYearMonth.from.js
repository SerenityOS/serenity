describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainYearMonth.from).toHaveLength(1);
    });

    test("PlainDate instance argument", () => {
        const plainDate = new Temporal.PlainDate(2021, 7, 6);
        const plainYearMonth = Temporal.PlainYearMonth.from(plainDate);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
        expect(plainYearMonth.daysInYear).toBe(365);
        expect(plainYearMonth.daysInMonth).toBe(31);
        expect(plainYearMonth.monthsInYear).toBe(12);
        expect(plainYearMonth.inLeapYear).toBeFalse();
    });

    test("PlainYearMonth instance argument", () => {
        const plainYearMonth_ = new Temporal.PlainYearMonth(2021, 7);
        const plainYearMonth = Temporal.PlainYearMonth.from(plainYearMonth_);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
        expect(plainYearMonth.daysInYear).toBe(365);
        expect(plainYearMonth.daysInMonth).toBe(31);
        expect(plainYearMonth.monthsInYear).toBe(12);
        expect(plainYearMonth.inLeapYear).toBeFalse();
    });

    test("ZonedDateTime instance argument", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(1625614921000000000n, timeZone);
        const plainYearMonth = Temporal.PlainYearMonth.from(zonedDateTime);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
        expect(plainYearMonth.daysInYear).toBe(365);
        expect(plainYearMonth.daysInMonth).toBe(31);
        expect(plainYearMonth.monthsInYear).toBe(12);
        expect(plainYearMonth.inLeapYear).toBeFalse();
    });

    test("fields object argument", () => {
        const object = {
            year: 2021,
            month: 7,
        };
        const plainYearMonth = Temporal.PlainYearMonth.from(object);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
        expect(plainYearMonth.daysInYear).toBe(365);
        expect(plainYearMonth.daysInMonth).toBe(31);
        expect(plainYearMonth.monthsInYear).toBe(12);
        expect(plainYearMonth.inLeapYear).toBeFalse();
    });

    test("from year month string", () => {
        const plainYearMonth = Temporal.PlainYearMonth.from("2021-07");
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
    });

    test("from date time string", () => {
        const plainYearMonth = Temporal.PlainYearMonth.from("2021-07-06T23:42:01");
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
        expect(plainYearMonth.monthCode).toBe("M07");
        expect(plainYearMonth.daysInYear).toBe(365);
        expect(plainYearMonth.daysInMonth).toBe(31);
        expect(plainYearMonth.monthsInYear).toBe(12);
        expect(plainYearMonth.inLeapYear).toBeFalse();
    });
});

describe("errors", () => {
    test("missing fields", () => {
        expect(() => {
            Temporal.PlainYearMonth.from({});
        }).toThrowWithMessage(TypeError, "Required property year is missing or undefined");
        expect(() => {
            Temporal.PlainYearMonth.from({ year: 0 });
        }).toThrowWithMessage(TypeError, "Required property month is missing or undefined");
        expect(() => {
            Temporal.PlainYearMonth.from({ month: 1 });
        }).toThrowWithMessage(TypeError, "Required property year is missing or undefined");
    });

    test("invalid year month string", () => {
        expect(() => {
            Temporal.PlainYearMonth.from("foo");
        }).toThrowWithMessage(RangeError, "Invalid year month string 'foo'");
    });

    test("string must not contain a UTC designator", () => {
        expect(() => {
            Temporal.PlainYearMonth.from("2021-07-06T23:42:01Z");
        }).toThrowWithMessage(
            RangeError,
            "Invalid year month string '2021-07-06T23:42:01Z': must not contain a UTC designator"
        );
    });
});
