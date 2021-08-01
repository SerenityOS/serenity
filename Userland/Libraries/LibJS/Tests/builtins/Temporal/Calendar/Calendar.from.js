describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.from).toHaveLength(1);
    });

    test("basic functionality", () => {
        const plainDate = new Temporal.PlainDate(1970, 1, 1);
        const plainTime = new Temporal.PlainTime();
        const plainDateTime = new Temporal.PlainDateTime(1970, 1, 1);
        // TODO: PlainMonthDay, PlainYearMonth, ZonedDateTime
        const calendarLike = {};
        const withCalendarLike = { calendar: {} };
        expect(Temporal.Calendar.from(plainDate)).toBe(plainDate.calendar);
        expect(Temporal.Calendar.from(plainTime)).toBe(plainTime.calendar);
        expect(Temporal.Calendar.from(plainDateTime)).toBe(plainDateTime.calendar);
        expect(Temporal.Calendar.from(calendarLike)).toBe(calendarLike);
        expect(Temporal.Calendar.from(withCalendarLike)).toBe(withCalendarLike.calendar);
        expect(Temporal.Calendar.from("iso8601").id).toBe("iso8601");
        // TODO: test Temporal.Calendar.from("TemporalCalendarString") once ParseTemporalCalendarString is working
    });
});
