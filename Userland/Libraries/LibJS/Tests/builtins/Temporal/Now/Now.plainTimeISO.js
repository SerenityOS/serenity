describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.Now.plainTimeISO).toHaveLength(0);
    });

    test("basic functionality", () => {
        const plainTime = Temporal.Now.plainTimeISO();
        expect(plainTime).toBeInstanceOf(Temporal.PlainTime);
        expect(plainTime.calendar.id).toBe("iso8601");
    });

    test("custom time zone", () => {
        const timeZone = {
            getOffsetNanosecondsFor() {
                return 86400000000000;
            },
        };
        const plainTime = Temporal.Now.plainTimeISO();
        const plainTimeWithOffset = Temporal.Now.plainTimeISO(timeZone);
        // FIXME: Compare these in a sensible way
    });
});

describe("errors", () => {
    test("custom time zone doesn't have a getOffsetNanosecondsFor function", () => {
        expect(() => {
            Temporal.Now.plainTimeISO({});
        }).toThrowWithMessage(TypeError, "null is not a function");
    });
});
