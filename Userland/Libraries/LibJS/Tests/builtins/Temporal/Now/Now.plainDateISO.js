describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.Now.plainDateISO).toHaveLength(0);
    });

    test("basic functionality", () => {
        const plainDate = Temporal.Now.plainDateISO();
        expect(plainDate).toBeInstanceOf(Temporal.PlainDate);
        expect(plainDate.calendar.id).toBe("iso8601");
    });

    test("custom time zone", () => {
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainDate = Temporal.Now.plainDateISO("UTC");
        const plainDateWithOffset = Temporal.Now.plainDateISO(timeZone);
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
            Temporal.Now.plainDateISO({});
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
