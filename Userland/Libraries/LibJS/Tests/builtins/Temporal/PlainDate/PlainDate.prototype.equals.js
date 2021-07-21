describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainDate.prototype.equals).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = { hello: "friends" };
        const first_plain_date = new Temporal.PlainDate(1, 1, 1, calendar);
        const second_plain_date = new Temporal.PlainDate(0, 1, 1, calendar);
        expect(first_plain_date.equals(first_plain_date));
        expect(!first_plain_date.equals(second_plain_date));
    });
});
