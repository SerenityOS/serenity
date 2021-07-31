describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.from).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const timeZoneLike = {};
        const zonedDateTimeLike = { timeZone: {} };
        expect(Temporal.TimeZone.from(timeZone)).toBe(timeZone);
        expect(Temporal.TimeZone.from(timeZoneLike)).toBe(timeZoneLike);
        expect(Temporal.TimeZone.from(zonedDateTimeLike)).toBe(zonedDateTimeLike.timeZone);
        // TODO: test from("string") once ParseTemporalTimeZoneString is working
    });
});
