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

    // Un-skip once ParseISODateTime & ParseTemporalDateString are implemented
    test.skip("PlainDate string argument", () => {
        const createdPlainDate = Temporal.PlainDate.from("2021-07-26");
        expect(createdPlainDate.year).toBe(2021);
        expect(createdPlainDate.month).toBe(7);
        expect(createdPlainDate.day).toBe(26);
    });
});
