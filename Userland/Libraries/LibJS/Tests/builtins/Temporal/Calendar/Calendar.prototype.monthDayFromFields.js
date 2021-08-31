describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Calendar.prototype.monthDayFromFields).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainMonthDay = calendar.monthDayFromFields({ year: 2021, month: 7, day: 6 });
        expect(plainMonthDay.calendar).toBe(calendar);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);

        const fields = plainMonthDay.getISOFields();
        expect(fields.isoYear).toBe(1972); // No, this isn't a mistake
    });

    test("with monthCode", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainMonthDay = calendar.monthDayFromFields({ monthCode: "M07", day: 6 });
        expect(plainMonthDay.calendar).toBe(calendar);
        expect(plainMonthDay.monthCode).toBe("M07");
        expect(plainMonthDay.day).toBe(6);

        const fields = plainMonthDay.getISOFields();
        expect(fields.isoYear).toBe(1972);
    });
});

describe("errors", () => {
    test("first argument must be an object", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.monthDayFromFields(42);
        }).toThrowWithMessage(TypeError, "42 is not an object");
    });

    test("month or monthCode field is required", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.monthDayFromFields({ year: 2021 });
        }).toThrowWithMessage(TypeError, "Required property month is missing or undefined");
    });

    test("day field is required", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.monthDayFromFields({ year: 2021, month: 7 });
        }).toThrowWithMessage(TypeError, "Required property day is missing or undefined");
    });

    test("monthCode or year field is required when month is given", () => {
        const calendar = new Temporal.Calendar("iso8601");
        expect(() => {
            calendar.monthDayFromFields({ month: 7, day: 6 });
        }).toThrowWithMessage(
            TypeError,
            "Required property monthCode or year is missing or undefined"
        );
    });
});
