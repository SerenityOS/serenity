describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainDate.from).toHaveLength(1);
    });

    test("PlainDate instance argument", () => {
        const plainDate = new Temporal.PlainDate(2021, 7, 26);
        const createdPlainDate = Temporal.PlainDate.from(plainDate);
        expect(createdPlainDate.year).toBe(2021);
        expect(createdPlainDate.month).toBe(7);
        expect(createdPlainDate.day).toBe(26);
    });

    test("PlainDateTime instance argument", () => {
        const plainDateTime = new Temporal.PlainDateTime(2021, 7, 26, 1, 2, 3);
        const createdPlainDate = Temporal.PlainDate.from(plainDateTime);
        expect(createdPlainDate.year).toBe(2021);
        expect(createdPlainDate.month).toBe(7);
        expect(createdPlainDate.day).toBe(26);
    });

    test("ZonedDateTime instance argument", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        const zonedDateTime = new Temporal.ZonedDateTime(1627318123456789000n, timeZone);
        const createdPlainDate = Temporal.PlainDate.from(zonedDateTime);
        expect(createdPlainDate.year).toBe(2021);
        expect(createdPlainDate.month).toBe(7);
        expect(createdPlainDate.day).toBe(26);
    });

    // Un-skip once ParseISODateTime & ParseTemporalDateString are implemented
    test.skip("PlainDate string argument", () => {
        const createdPlainDate = Temporal.PlainDate.from("2021-07-26");
        expect(createdPlainDate.year).toBe(2021);
        expect(createdPlainDate.month).toBe(7);
        expect(createdPlainDate.day).toBe(26);
    });
});
