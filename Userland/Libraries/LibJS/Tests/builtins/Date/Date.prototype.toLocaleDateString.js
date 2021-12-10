describe("errors", () => {
    test("called on non-Date object", () => {
        expect(() => {
            Date.prototype.toLocaleDateString();
        }).toThrowWithMessage(TypeError, "Not an object of type Date");
    });

    test("called with value that cannot be converted to a number", () => {
        expect(() => {
            new Date(Symbol.hasInstance).toLocaleDateString();
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to number");

        expect(() => {
            new Date(1n).toLocaleDateString();
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");
    });

    test("time value cannot be clipped", () => {
        expect(() => {
            new Date(-8.65e15).toLocaleDateString();
        }).toThrowWithMessage(RangeError, "Time value must be between -8.64E15 and 8.64E15");
    });

    test("timeStyle may not be specified", () => {
        expect(() => {
            new Date().toLocaleDateString([], { timeStyle: "short" });
        }).toThrowWithMessage(TypeError, "Option timeStyle cannot be set when also providing date");
    });
});

describe("correct behavior", () => {
    test("NaN", () => {
        const d = new Date(NaN);
        expect(d.toLocaleDateString()).toBe("Invalid Date");
    });

    const d0 = new Date(Date.UTC(2021, 11, 7, 17, 40, 50, 456));
    const d1 = new Date(Date.UTC(1989, 0, 23, 7, 8, 9, 45));

    test("defaults to date", () => {
        expect(d0.toLocaleDateString("en")).toBe("12/7/2021");
        expect(d1.toLocaleDateString("en")).toBe("1/23/1989");

        expect(d0.toLocaleDateString("ar")).toBe("٧‏/١٢‏/٢٠٢١");
        expect(d1.toLocaleDateString("ar")).toBe("٢٣‏/١‏/١٩٨٩");
    });

    test("dateStyle may be set", () => {
        expect(d0.toLocaleDateString("en", { dateStyle: "full" })).toBe(
            "Tuesday, December 7, 2021"
        );
        expect(d1.toLocaleDateString("en", { dateStyle: "full" })).toBe("Monday, January 23, 1989");

        expect(d0.toLocaleDateString("ar", { dateStyle: "full" })).toBe("الثلاثاء، ٧ ديسمبر ٢٠٢١");
        expect(d1.toLocaleDateString("ar", { dateStyle: "full" })).toBe("الاثنين، ٢٣ يناير ١٩٨٩");
    });
});
