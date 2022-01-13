describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Temporal.Now.plainDate).toHaveLength(1);
    });

    test("basic functionality", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const plainDate = Temporal.Now.plainDate(calendar);
        expect(plainDate).toBeInstanceOf(Temporal.PlainDate);
        expect(plainDate.calendar).toBe(calendar);
    });

    test("custom time zone", () => {
        const calendar = new Temporal.Calendar("iso8601");
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainDate = Temporal.Now.plainDate(calendar, "UTC");
        const plainDateWithOffset = Temporal.Now.plainDate(calendar, timeZone);
        if (plainDate.dayOfYear === plainDate.daysInYear) {
            expect(plainDateWithOffset.year).toBe(plainDate.year + 1);
            expect(plainDateWithOffset.month).toBe(1);
            expect(plainDateWithOffset.day).toBe(1);
        } else {
            expect(plainDateWithOffset.year).toBe(plainDate.year);
            if (plainDate.day === plainDate.daysInMonth) {
                expect(plainDateWithOffset.month).toBe(plainDate.month + 1);
                expect(plainDateWithOffset.day).toBe(1);
            } else {
                expect(plainDateWithOffset.month).toBe(plainDate.month);
                expect(plainDateWithOffset.day).toBe(plainDate.day + 1);
            }
        }
    });
});

describe("errors", () => {
    test("custom time zone doesn't have a getOffsetNanosecondsFor function", () => {
        expect(() => {
            Temporal.Now.plainDate({}, {});
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
