describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.ZonedDateTime.prototype.until).toHaveLength(1);
    });

    test("basic functionality", () => {
        const values = [
            [0n, 0n, "PT0S"],
            [123456789n, 2345679011n, "PT2.222222222S"],
            [0n, 123456789n, "PT0.123456789S"],
            [123456789n, 0n, "-PT0.123456789S"],
            [0n, 123456789123456789n, "PT34293H33M9.123456789S"],
            [123456789123456789n, 0n, "-PT34293H33M9.123456789S"],
        ];
        const utc = new Temporal.TimeZone("UTC");
        for (const [arg, argOther, expected] of values) {
            const zonedDateTime = new Temporal.ZonedDateTime(arg, utc);
            const other = new Temporal.ZonedDateTime(argOther, utc);
            expect(zonedDateTime.until(other).toString()).toBe(expected);
        }
    });

    test("smallestUnit option", () => {
        const utc = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(0n, utc);
        const other = new Temporal.ZonedDateTime(34401906007008009n, utc);
        const values = [
            ["year", "P1Y"],
            ["month", "P13M"],
            ["week", "P56W"],
            ["day", "P398D"],
            ["hour", "PT9556H"],
            ["minute", "PT9556H5M"],
            ["second", "PT9556H5M6S"],
            ["millisecond", "PT9556H5M6.007S"],
            ["microsecond", "PT9556H5M6.007008S"],
            ["nanosecond", "PT9556H5M6.007008009S"],
        ];
        for (const [smallestUnit, expected] of values) {
            expect(zonedDateTime.until(other, { smallestUnit }).toString()).toBe(expected);
        }
    });

    test("largestUnit option", () => {
        const utc = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(0n, utc);
        const other = new Temporal.ZonedDateTime(34401906007008009n, utc);
        const values = [
            ["year", "P1Y1M2DT4H5M6.007008009S"],
            ["month", "P13M2DT4H5M6.007008009S"],
            ["week", "P56W6DT4H5M6.007008009S"],
            ["day", "P398DT4H5M6.007008009S"],
            ["hour", "PT9556H5M6.007008009S"],
            ["minute", "PT573365M6.007008009S"],
            ["second", "PT34401906.007008009S"],
            ["millisecond", "PT34401906.007008009S"],
            ["microsecond", "PT34401906.007008009S"],
            ["nanosecond", "PT34401906.007008008S"],
        ];
        for (const [largestUnit, expected] of values) {
            expect(zonedDateTime.until(other, { largestUnit }).toString()).toBe(expected);
        }
    });
});

describe("errors", () => {
    test("this value must be a Temporal.ZonedDateTime object", () => {
        expect(() => {
            Temporal.ZonedDateTime.prototype.until.call("foo", {});
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.ZonedDateTime");
    });

    test("cannot compare dates from different calendars", () => {
        const calendarOne = {
            toString() {
                return "calendarOne";
            },
        };

        const calendarTwo = {
            toString() {
                return "calendarTwo";
            },
        };

        const utc = new Temporal.TimeZone("UTC");
        const zonedDateTimeOne = new Temporal.ZonedDateTime(0n, utc, calendarOne);
        const zonedDateTimeTwo = new Temporal.ZonedDateTime(0n, utc, calendarTwo);

        expect(() => {
            zonedDateTimeOne.until(zonedDateTimeTwo);
        }).toThrowWithMessage(RangeError, "Cannot compare dates from two different calendars");
    });

    test("cannot compare dates from different time zones", () => {
        const timeZoneOne = {
            toString() {
                return "timeZoneOne";
            },
        };

        const timeZoneTwo = {
            toString() {
                return "timeZoneTwo";
            },
        };

        const zonedDateTimeOne = new Temporal.ZonedDateTime(0n, timeZoneOne);
        const zonedDateTimeTwo = new Temporal.ZonedDateTime(0n, timeZoneTwo);

        expect(() => {
            zonedDateTimeOne.until(zonedDateTimeTwo, { largestUnit: "day" });
        }).toThrowWithMessage(RangeError, "Cannot compare dates from two different time zones");
    });
});
