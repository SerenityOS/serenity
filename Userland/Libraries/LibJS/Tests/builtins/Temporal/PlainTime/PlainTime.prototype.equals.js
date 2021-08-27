describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.PlainTime.prototype.equals).toHaveLength(1);
    });

    test("basic functionality", () => {
        const firstPlainTime = new Temporal.PlainTime(1, 1, 1, 1, 1, 1);
        const secondPlainTime = new Temporal.PlainTime(0, 1, 1, 1, 1, 1);
        expect(firstPlainTime.equals(firstPlainTime));
        expect(!secondPlainTime.equals(secondPlainTime));
    });
});
