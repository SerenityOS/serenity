describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.from).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const plainDate = new Temporal.PlainDate(1970, 1, 1);
        const plainTime = new Temporal.PlainTime();
        const plainDateTime = new Temporal.PlainDateTime(1970, 1, 1);
        const plainMonthDay = new Temporal.PlainMonthDay(1, 1);
        const plainYearMonth = new Temporal.PlainYearMonth(1970, 1);
        const zonedDateTime = new Temporal.ZonedDateTime(0n, timeZone);
        const calendarLike = {};
        const withCalendarLike = { calendar: {} };
        expect(Temporal.Calendar.from(plainDate)).toBe(plainDate.calendar);
        expect(Temporal.Calendar.from(plainTime)).toBe(plainTime.calendar);
        expect(Temporal.Calendar.from(plainDateTime)).toBe(plainDateTime.calendar);
        expect(Temporal.Calendar.from(plainMonthDay)).toBe(plainMonthDay.calendar);
        expect(Temporal.Calendar.from(plainYearMonth)).toBe(plainYearMonth.calendar);
        expect(Temporal.Calendar.from(zonedDateTime)).toBe(zonedDateTime.calendar);
        expect(Temporal.Calendar.from(calendarLike)).toBe(calendarLike);
        expect(Temporal.Calendar.from(withCalendarLike)).toBe(withCalendarLike.calendar);
        expect(Temporal.Calendar.from("iso8601").id).toBe("iso8601");
        expect(Temporal.Calendar.from("2021-07-06[u-ca=iso8601]").id).toBe("iso8601");
    });
});
