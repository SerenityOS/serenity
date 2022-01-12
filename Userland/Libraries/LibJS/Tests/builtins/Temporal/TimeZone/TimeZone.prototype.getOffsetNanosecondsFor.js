describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone.prototype.getOffsetNanosecondsFor).toHaveLength(1);
    });

    test("basic functionality", () => {
        // Adapted from TestTimeZone.cpp's TEST_CASE(get_time_zone_offset).

        function offset(sign, hours, minutes, seconds) {
            return sign * (hours * 3600 + minutes * 60 + seconds) * 1_000_000_000;
        }

        function testOffset(timeZone, time, expectedOffset) {
            const instant = new Temporal.Instant(BigInt(time) * 1_000_000_000n);
            const actualOffset = new Temporal.TimeZone(timeZone).getOffsetNanosecondsFor(instant);
            expect(actualOffset).toBe(expectedOffset);
        }

        testOffset("America/Chicago", -2717668237, offset(-1, 5, 50, 36)); // Sunday, November 18, 1883 12:09:23 PM
        testOffset("America/Chicago", -2717668236, offset(-1, 6, 0, 0)); // Sunday, November 18, 1883 12:09:24 PM
        testOffset("America/Chicago", -1067810460, offset(-1, 6, 0, 0)); // Sunday, March 1, 1936 1:59:00 AM
        testOffset("America/Chicago", -1067810400, offset(-1, 5, 0, 0)); // Sunday, March 1, 1936 2:00:00 AM
        testOffset("America/Chicago", -1045432860, offset(-1, 5, 0, 0)); // Sunday, November 15, 1936 1:59:00 AM
        testOffset("America/Chicago", -1045432800, offset(-1, 6, 0, 0)); // Sunday, November 15, 1936 2:00:00 AM

        testOffset("Europe/London", -3852662401, offset(-1, 0, 1, 15)); // Tuesday, November 30, 1847 11:59:59 PM
        testOffset("Europe/London", -3852662400, offset(+1, 0, 0, 0)); // Wednesday, December 1, 1847 12:00:00 AM
        testOffset("Europe/London", -37238401, offset(+1, 0, 0, 0)); // Saturday, October 26, 1968 11:59:59 PM
        testOffset("Europe/London", -37238400, offset(+1, 1, 0, 0)); // Sunday, October 27, 1968 12:00:00 AM
        testOffset("Europe/London", 57722399, offset(+1, 1, 0, 0)); // Sunday, October 31, 1971 1:59:59 AM
        testOffset("Europe/London", 57722400, offset(+1, 0, 0, 0)); // Sunday, October 31, 1971 2:00:00 AM

        testOffset("UTC", -1641846268, offset(+1, 0, 0, 0));
        testOffset("UTC", 0, offset(+1, 0, 0, 0));
        testOffset("UTC", 1641846268, offset(+1, 0, 0, 0));

        testOffset("Etc/GMT+4", -1641846268, offset(-1, 4, 0, 0));
        testOffset("Etc/GMT+5", 0, offset(-1, 5, 0, 0));
        testOffset("Etc/GMT+6", 1641846268, offset(-1, 6, 0, 0));

        testOffset("Etc/GMT-12", -1641846268, offset(+1, 12, 0, 0));
        testOffset("Etc/GMT-13", 0, offset(+1, 13, 0, 0));
        testOffset("Etc/GMT-14", 1641846268, offset(+1, 14, 0, 0));
    });

    test("custom offset", () => {
        const timeZone = new Temporal.TimeZone("+01:30");
        const instant = new Temporal.Instant(0n);
        expect(timeZone.getOffsetNanosecondsFor(instant)).toBe(5400000000000);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.TimeZone object", () => {
        expect(() => {
            Temporal.TimeZone.prototype.getOffsetNanosecondsFor.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.TimeZone");
    });
});
