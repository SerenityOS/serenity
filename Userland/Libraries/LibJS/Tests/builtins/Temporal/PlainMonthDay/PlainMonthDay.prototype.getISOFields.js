describe("normal behavior", () => {
    test("length is 0", () => {
        expect(Temporal.PlainMonthDay.prototype.getISOFields).toHaveLength(0);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainMonthDay = new Temporal.PlainMonthDay(7, 6, calendar, 2021);
        const fields = plainMonthDay.getISOFields();
        expect(fields).toEqual({ calendar, isoDay: 6, isoMonth: 7, isoYear: 2021 });
        // Test field order
        expect(Object.getOwnPropertyNames(fields)).toEqual([
            "calendar",
            "isoDay",
            "isoMonth",
            "isoYear",
        ]);
    });
});

describe("errors", () => {
    test("this value must be a Temporal.PlainMonthDay object", () => {
        expect(() => {
            Temporal.PlainMonthDay.prototype.getISOFields.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.PlainMonthDay");
    });
});
