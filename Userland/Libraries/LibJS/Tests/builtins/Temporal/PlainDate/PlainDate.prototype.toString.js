describe("correct behavior", () => {
    test("length is 0", () => {
        expect(Temporal.PlainDate.prototype.toString).toHaveLength(0);
    });

    test("basic functionality", () => {
        let plainDate;

        plainDate = new Temporal.PlainDate(2021, 7, 6);
        expect(plainDate.toString()).toBe("2021-07-06");
        expect(plainDate.toString({ calendarName: "auto" })).toBe("2021-07-06");
        expect(plainDate.toString({ calendarName: "always" })).toBe("2021-07-06[u-ca=iso8601]");
        expect(plainDate.toString({ calendarName: "never" })).toBe("2021-07-06");

        plainDate = new Temporal.PlainDate(2021, 7, 6, { toString: () => "foo" });
        expect(plainDate.toString()).toBe("2021-07-06[u-ca=foo]");
        expect(plainDate.toString({ calendarName: "auto" })).toBe("2021-07-06[u-ca=foo]");
        expect(plainDate.toString({ calendarName: "always" })).toBe("2021-07-06[u-ca=foo]");
        expect(plainDate.toString({ calendarName: "never" })).toBe("2021-07-06");

        plainDate = new Temporal.PlainDate(0, 1, 1);
        expect(plainDate.toString()).toBe("+000000-01-01");

        plainDate = new Temporal.PlainDate(999, 1, 1);
        expect(plainDate.toString()).toBe("+000999-01-01");

        plainDate = new Temporal.PlainDate(12345, 1, 1);
        expect(plainDate.toString()).toBe("+012345-01-01");

        plainDate = new Temporal.PlainDate(123456, 1, 1);
        expect(plainDate.toString()).toBe("+123456-01-01");

        plainDate = new Temporal.PlainDate(-12345, 1, 1);
        expect(plainDate.toString()).toBe("-012345-01-01");
    });
});

describe("errors", () => {
    test("this value must be a Temporal.PlainDate object", () => {
        expect(() => {
            Temporal.PlainDate.prototype.toString.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type Temporal.PlainDate");
    });

    test("calendarName option must be one of 'auto', 'always', 'never'", () => {
        const plainDate = new Temporal.PlainDate(2021, 7, 6);
        expect(() => {
            plainDate.toString({ calendarName: "foo" });
        }).toThrowWithMessage(RangeError, "foo is not a valid value for option calendarName");
    });
});
