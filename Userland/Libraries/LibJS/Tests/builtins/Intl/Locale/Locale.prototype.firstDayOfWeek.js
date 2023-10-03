describe("errors", () => {
    test("called on non-Locale object", () => {
        expect(() => {
            Intl.Locale.prototype.firstDayOfWeek;
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.Locale");
    });

    test("invalid options", () => {
        [100, Infinity, NaN, "hello", 152n, true].forEach(value => {
            expect(() => {
                new Intl.Locale("en", { firstDayOfWeek: value }).firstDayOfWeek;
            }).toThrowWithMessage(
                RangeError,
                `${value} is not a valid value for option firstDayOfWeek`
            );
        });
    });

    // FIXME: Spec issue: It is not yet clear if the following invalid values should throw. For now, this tests our
    //        existing workaround behavior, which returns "undefined" for invalid extensions.
    //        https://github.com/tc39/proposal-intl-locale-info/issues/78
    test("invalid extensions", () => {
        [100, Infinity, NaN, "hello", 152n, true].forEach(value => {
            expect(new Intl.Locale(`en-u-fw-${value}`).firstDayOfWeek).toBeUndefined();
        });
    });
});

describe("normal behavior", () => {
    test("valid options", () => {
        expect(new Intl.Locale("en").firstDayOfWeek).toBeUndefined();

        ["mon", "tue", "wed", "thu", "fri", "sat", "sun"].forEach((day, index) => {
            expect(new Intl.Locale(`en-u-fw-${day}`).firstDayOfWeek).toBe(index + 1);
            expect(new Intl.Locale("en", { firstDayOfWeek: day }).firstDayOfWeek).toBe(index + 1);
            expect(new Intl.Locale("en", { firstDayOfWeek: index + 1 }).firstDayOfWeek).toBe(
                index + 1
            );
            expect(new Intl.Locale("en-u-fw-mon", { firstDayOfWeek: day }).firstDayOfWeek).toBe(
                index + 1
            );
        });
    });
});
