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
        expect(Temporal.TimeZone.from("UTC").id).toBe("UTC");
        expect(Temporal.TimeZone.from("GMT").id).toBe("UTC");
        expect(Temporal.TimeZone.from("Etc/UTC").id).toBe("UTC");
        expect(Temporal.TimeZone.from("Etc/GMT").id).toBe("UTC");
        // FIXME: https://github.com/tc39/proposal-temporal/issues/1993
        // expect(Temporal.TimeZone.from("Etc/GMT+12").id).toBe("Etc/GMT+12");
        // expect(Temporal.TimeZone.from("Etc/GMT-12").id).toBe("Etc/GMT-12");
        expect(Temporal.TimeZone.from("Europe/London").id).toBe("Europe/London");
        expect(Temporal.TimeZone.from("Europe/Isle_of_Man").id).toBe("Europe/London");
    });
});
