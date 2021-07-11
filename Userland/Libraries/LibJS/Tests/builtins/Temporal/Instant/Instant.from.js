describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Instant.from).toHaveLength(1);
    });

    test("Instant instance argument", () => {
        const instant = new Temporal.Instant(123n);
        expect(Temporal.Instant.from(instant).epochNanoseconds).toBe(123n);
    });

    // Un-skip once ParseISODateTime & ParseTemporalTimeZoneString are implemented
    test.skip("Instant string argument", () => {
        expect(Temporal.Instant.from("1975-02-02T14:25:36.123456789Z").epochNanoseconds).toBe(
            160583136123456789n
        );
    });
});
