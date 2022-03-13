describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Instant.from).toHaveLength(1);
    });

    test("Instant instance argument", () => {
        const instant = new Temporal.Instant(123n);
        expect(Temporal.Instant.from(instant).epochNanoseconds).toBe(123n);
    });

    test("ZonedDateTime instance argument", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(123n, timeZone);
        expect(Temporal.Instant.from(zonedDateTime).epochNanoseconds).toBe(123n);
    });

    test("Instant string argument", () => {
        expect(Temporal.Instant.from("1975-02-02T14:25:36.123456789Z").epochNanoseconds).toBe(
            160583136123456789n
        );
        // Time zone is not validated
        expect(
            Temporal.Instant.from("1975-02-02T14:25:36.123456789Z[Custom/TimeZone]")
                .epochNanoseconds
        ).toBe(160583136123456789n);
    });
});

describe("errors", () => {
    test("invalid instant string", () => {
        expect(() => {
            Temporal.Instant.from("foo");
        }).toThrowWithMessage(RangeError, "Invalid instant string 'foo'");
    });

    test("invalid epoch nanoseconds", () => {
        // Test cases from https://github.com/tc39/proposal-temporal/commit/baead4d85bc3e9ecab1e9824c3d3fe4fdd77fc3a
        expect(() => {
            Temporal.Instant.from("-271821-04-20T00:00:00+00:01");
        }).toThrowWithMessage(
            RangeError,
            "Invalid epoch nanoseconds value, must be in range -86400 * 10^17 to 86400 * 10^17"
        );
        expect(() => {
            Temporal.Instant.from("+275760-09-13T00:00:00-00:01");
        }).toThrowWithMessage(
            RangeError,
            "Invalid epoch nanoseconds value, must be in range -86400 * 10^17 to 86400 * 10^17"
        );
    });
});
