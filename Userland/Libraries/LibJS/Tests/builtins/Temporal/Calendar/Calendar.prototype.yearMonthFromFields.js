describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.prototype.yearMonthFromFields).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainYearMonth = calendar.yearMonthFromFields({ year: 2021, month: 7 });
        expect(plainYearMonth.calendar).toBe(calendar);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
    });

    test("with monthCode", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainYearMonth = calendar.yearMonthFromFields({ year: 2021, monthCode: "M07" });
        expect(plainYearMonth.calendar).toBe(calendar);
        expect(plainYearMonth.year).toBe(2021);
        expect(plainYearMonth.month).toBe(7);
    });
});

describe("errors", () => {
    test("first argument must be an object", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.yearMonthFromFields(42);
        }).toThrowWithMessage(TypeError, "42 is not an object");
    });

    test("year field is required", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.yearMonthFromFields({ month: 7 });
        }).toThrowWithMessage(TypeError, "Required property year is missing or undefined");
    });

    test("month or monthCode field is required", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.yearMonthFromFields({ year: 2021 });
        }).toThrowWithMessage(TypeError, "Required property month is missing or undefined");
    });
});
