describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.from).toHaveLength(1);
    });

    test("basic functionality", () => {
        // From object
        const timeZone = new Temporal.TimeZone("UTC");
        const timeZoneLike = {};
        const zonedDateTimeLike = { timeZone: {} };
        expect(Temporal.TimeZone.from(timeZone)).toBe(timeZone);
        expect(Temporal.TimeZone.from(timeZoneLike)).toBe(timeZoneLike);
        expect(Temporal.TimeZone.from(zonedDateTimeLike)).toBe(zonedDateTimeLike.timeZone);

        // From string
        const values = [
            ["UTC", "UTC"],
            ["GMT", "UTC"],
            ["Etc/UTC", "UTC"],
            ["Etc/GMT", "UTC"],
            // FIXME: https://github.com/tc39/proposal-temporal/issues/1993
            // ["Etc/GMT+12", "Etc/GMT+12"],
            // ["Etc/GMT-12", "Etc/GMT-12"],
            ["Europe/London", "Europe/London"],
            ["Europe/Isle_of_Man", "Europe/London"],
            ["1970-01-01+01", "+01:00"],
            ["1970-01-01+01[-12:34]", "+01:00"],
            ["1970-01-01T00:00:00+01", "+01:00"],
            ["1970-01-01T00:00:00.000000000+01", "+01:00"],
            ["1970-01-01T00:00:00.000000000+01:00:00", "+01:00"],
            ["1970-01-01+12:34", "+12:34"],
            ["1970-01-01+12:34:56", "+12:34:56"],
            ["1970-01-01+12:34:56.789", "+12:34:56.789"],
            ["1970-01-01+12:34:56.789[-01:00]", "+12:34:56.789"],
            ["1970-01-01-12:34", "-12:34"],
            ["1970-01-01-12:34:56", "-12:34:56"],
            ["1970-01-01-12:34:56.789", "-12:34:56.789"],
            ["1970-01-01-12:34:56.789[+01:00]", "-12:34:56.789"],
        ];
        for (const [arg, expected] of values) {
            expect(Temporal.TimeZone.from(arg).id).toBe(expected);
        }
    });
});
