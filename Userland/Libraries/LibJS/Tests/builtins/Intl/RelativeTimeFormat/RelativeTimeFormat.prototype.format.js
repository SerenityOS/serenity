describe("errors", () => {
    test("called on non-RelativeimeFormat object", () => {
        expect(() => {
            Intl.RelativeTimeFormat.prototype.format(1);
        }).toThrowWithMessage(TypeError, "Not an object of type Intl.RelativeTimeFormat");
    });

    test("called with value that cannot be converted to a number", () => {
        expect(() => {
            new Intl.RelativeTimeFormat().format(Symbol.hasInstance, "year");
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to number");

        expect(() => {
            new Intl.RelativeTimeFormat().format(1n, "year");
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");
    });

    test("called with unit that cannot be converted to a number", () => {
        expect(() => {
            new Intl.RelativeTimeFormat().format(1, Symbol.hasInstance);
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to string");
    });

    test("called with non-finite value", () => {
        expect(() => {
            new Intl.RelativeTimeFormat().format(Infinity, "year");
        }).toThrowWithMessage(RangeError, "Number must not be NaN or Infinity");

        expect(() => {
            new Intl.RelativeTimeFormat().format(-Infinity, "year");
        }).toThrowWithMessage(RangeError, "Number must not be NaN or Infinity");

        expect(() => {
            new Intl.RelativeTimeFormat().format(NaN, "year");
        }).toThrowWithMessage(RangeError, "Number must not be NaN or Infinity");
    });

    test("called with non-sanctioned unit", () => {
        expect(() => {
            new Intl.RelativeTimeFormat().format(1);
        }).toThrowWithMessage(RangeError, "Unit undefined is not a valid time unit");

        expect(() => {
            new Intl.RelativeTimeFormat().format(1, null);
        }).toThrowWithMessage(RangeError, "Unit null is not a valid time unit");

        expect(() => {
            new Intl.RelativeTimeFormat().format(1, "hello!");
        }).toThrowWithMessage(RangeError, "Unit hello! is not a valid time unit");
    });
});

let formatters = {};

["en", "ar"].forEach(locale => {
    formatters[locale] = {};

    ["long", "short", "narrow"].forEach(style => {
        formatters[locale][style] = {};

        ["always", "auto"].forEach(numeric => {
            formatters[locale][style][numeric] = new Intl.RelativeTimeFormat(locale, {
                style: style,
                numeric: numeric,
            });
        });
    });
});

function runTest(unit, style, numeric, en, ar) {
    const pluralUnit = `${unit}s`;

    [-2, -1, -0, 0, 1, 2].forEach((value, i) => {
        expect(formatters["en"][style][numeric].format(value, unit)).toBe(en[i]);
        expect(formatters["ar"][style][numeric].format(value, unit)).toBe(ar[i]);

        expect(formatters["en"][style][numeric].format(value, pluralUnit)).toBe(en[i]);
        expect(formatters["ar"][style][numeric].format(value, pluralUnit)).toBe(ar[i]);
    });
}

describe("second", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 seconds ago", "1 second ago", "0 seconds ago", "in 0 seconds", "in 1 second", "in 2 seconds" ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "قبل ٠ ثانية", "خلال ٠ ثانية", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 sec. ago", "1 sec. ago", "0 sec. ago", "in 0 sec.", "in 1 sec.", "in 2 sec." ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "قبل ٠ ثانية", "خلال ٠ ثانية", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 sec. ago", "1 sec. ago", "0 sec. ago", "in 0 sec.", "in 1 sec.", "in 2 sec." ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "قبل ٠ ثانية", "خلال ٠ ثانية", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 seconds ago", "1 second ago", "now", "now", "in 1 second", "in 2 seconds" ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "الآن", "الآن", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 sec. ago", "1 sec. ago", "now", "now", "in 1 sec.", "in 2 sec." ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "الآن", "الآن", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 sec. ago", "1 sec. ago", "now", "now", "in 1 sec.", "in 2 sec." ]; // prettier-ignore
        const ar = [ "قبل ثانيتين", "قبل ثانية واحدة", "الآن", "الآن", "خلال ثانية واحدة", "خلال ثانيتين" ]; // prettier-ignore

        runTest("second", "narrow", "auto", en, ar);
    });
});

describe("minute", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 minutes ago", "1 minute ago", "0 minutes ago", "in 0 minutes", "in 1 minute", "in 2 minutes" ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "قبل ٠ دقيقة", "خلال ٠ دقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 min. ago", "1 min. ago", "0 min. ago", "in 0 min.", "in 1 min.", "in 2 min." ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "قبل ٠ دقيقة", "خلال ٠ دقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 min. ago", "1 min. ago", "0 min. ago", "in 0 min.", "in 1 min.", "in 2 min." ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "قبل ٠ دقيقة", "خلال ٠ دقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 minutes ago", "1 minute ago", "this minute", "this minute", "in 1 minute", "in 2 minutes" ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "هذه الدقيقة", "هذه الدقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 min. ago", "1 min. ago", "this minute", "this minute", "in 1 min.", "in 2 min." ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "هذه الدقيقة", "هذه الدقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 min. ago", "1 min. ago", "this minute", "this minute", "in 1 min.", "in 2 min." ]; // prettier-ignore
        const ar = [ "قبل دقيقتين", "قبل دقيقة واحدة", "هذه الدقيقة", "هذه الدقيقة", "خلال دقيقة واحدة", "خلال دقيقتين" ]; // prettier-ignore

        runTest("minute", "narrow", "auto", en, ar);
    });
});

describe("hour", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 hours ago", "1 hour ago", "0 hours ago", "in 0 hours", "in 1 hour", "in 2 hours" ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "قبل ٠ ساعة", "خلال ٠ ساعة", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 hr. ago", "1 hr. ago", "0 hr. ago", "in 0 hr.", "in 1 hr.", "in 2 hr." ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "قبل ٠ ساعة", "خلال ٠ ساعة", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 hr. ago", "1 hr. ago", "0 hr. ago", "in 0 hr.", "in 1 hr.", "in 2 hr." ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "قبل ٠ ساعة", "خلال ٠ ساعة", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 hours ago", "1 hour ago", "this hour", "this hour", "in 1 hour", "in 2 hours" ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "الساعة الحالية", "الساعة الحالية", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 hr. ago", "1 hr. ago", "this hour", "this hour", "in 1 hr.", "in 2 hr." ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "الساعة الحالية", "الساعة الحالية", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 hr. ago", "1 hr. ago", "this hour", "this hour", "in 1 hr.", "in 2 hr." ]; // prettier-ignore
        const ar = [ "قبل ساعتين", "قبل ساعة واحدة", "الساعة الحالية", "الساعة الحالية", "خلال ساعة واحدة", "خلال ساعتين" ]; // prettier-ignore

        runTest("hour", "narrow", "auto", en, ar);
    });
});

describe("day", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 days ago", "1 day ago", "0 days ago", "in 0 days", "in 1 day", "in 2 days" ]; // prettier-ignore
        const ar = [ "قبل يومين", "قبل يوم واحد", "قبل ٠ يوم", "خلال ٠ يوم", "خلال يوم واحد", "خلال يومين" ]; // prettier-ignore

        runTest("day", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 days ago", "1 day ago", "0 days ago", "in 0 days", "in 1 day", "in 2 days" ]; // prettier-ignore
        const ar = [ "قبل يومين", "قبل يوم واحد", "قبل ٠ يوم", "خلال ٠ يوم", "خلال يوم واحد", "خلال يومين" ]; // prettier-ignore

        runTest("day", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 days ago", "1 day ago", "0 days ago", "in 0 days", "in 1 day", "in 2 days" ]; // prettier-ignore
        const ar = [ "قبل يومين", "قبل يوم واحد", "قبل ٠ يوم", "خلال ٠ يوم", "خلال يوم واحد", "خلال يومين" ]; // prettier-ignore

        runTest("day", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 days ago", "yesterday", "today", "today", "tomorrow", "in 2 days" ]; // prettier-ignore
        const ar = [ "أول أمس", "أمس", "اليوم", "اليوم", "غدًا", "بعد الغد" ]; // prettier-ignore

        runTest("day", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 days ago", "yesterday", "today", "today", "tomorrow", "in 2 days" ]; // prettier-ignore
        const ar = [ "أول أمس", "أمس", "اليوم", "اليوم", "غدًا", "بعد الغد" ]; // prettier-ignore

        runTest("day", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 days ago", "yesterday", "today", "today", "tomorrow", "in 2 days" ]; // prettier-ignore
        const ar = [ "أول أمس", "أمس", "اليوم", "اليوم", "غدًا", "بعد الغد" ]; // prettier-ignore

        runTest("day", "narrow", "auto", en, ar);
    });
});

describe("week", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 weeks ago", "1 week ago", "0 weeks ago", "in 0 weeks", "in 1 week", "in 2 weeks" ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "قبل أسبوع واحد", "قبل ٠ أسبوع", "خلال ٠ أسبوع", "خلال أسبوع واحد", "خلال أسبوعين" ]; // prettier-ignore

        runTest("week", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 wk. ago", "1 wk. ago", "0 wk. ago", "in 0 wk.", "in 1 wk.", "in 2 wk." ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "قبل أسبوع واحد", "قبل ٠ أسبوع", "خلال ٠ أسبوع", "خلال أسبوع واحد", "خلال ٢ أسبوعين" ]; // prettier-ignore

        runTest("week", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 wk. ago", "1 wk. ago", "0 wk. ago", "in 0 wk.", "in 1 wk.", "in 2 wk." ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "قبل أسبوع واحد", "قبل ٠ أسبوع", "خلال ٠ أسبوع", "خلال أسبوع واحد", "خلال أسبوعين" ]; // prettier-ignore

        runTest("week", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 weeks ago", "last week", "this week", "this week", "next week", "in 2 weeks" ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "الأسبوع الماضي", "هذا الأسبوع", "هذا الأسبوع", "الأسبوع القادم", "خلال أسبوعين" ]; // prettier-ignore

        runTest("week", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 wk. ago", "last wk.", "this wk.", "this wk.", "next wk.", "in 2 wk." ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "الأسبوع الماضي", "هذا الأسبوع", "هذا الأسبوع", "الأسبوع القادم", "خلال ٢ أسبوعين" ]; // prettier-ignore

        runTest("week", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 wk. ago", "last wk.", "this wk.", "this wk.", "next wk.", "in 2 wk." ]; // prettier-ignore
        const ar = [ "قبل أسبوعين", "الأسبوع الماضي", "هذا الأسبوع", "هذا الأسبوع", "الأسبوع القادم", "خلال أسبوعين" ]; // prettier-ignore

        runTest("week", "narrow", "auto", en, ar);
    });
});

describe("month", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 months ago", "1 month ago", "0 months ago", "in 0 months", "in 1 month", "in 2 months" ]; // prettier-ignore
        const ar = [ "قبل شهرين", "قبل شهر واحد", "قبل ٠ شهر", "خلال ٠ شهر", "خلال شهر واحد", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 mo. ago", "1 mo. ago", "0 mo. ago", "in 0 mo.", "in 1 mo.", "in 2 mo." ]; // prettier-ignore
        const ar = [ "قبل شهرين", "قبل شهر واحد", "قبل ٠ شهر", "خلال ٠ شهر", "خلال شهر واحد", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 mo. ago", "1 mo. ago", "0 mo. ago", "in 0 mo.", "in 1 mo.", "in 2 mo." ]; // prettier-ignore
        const ar = [ "قبل شهرين", "قبل شهر واحد", "قبل ٠ شهر", "خلال ٠ شهر", "خلال شهر واحد", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 months ago", "last month", "this month", "this month", "next month", "in 2 months" ]; // prettier-ignore
        const ar = [ "قبل شهرين", "الشهر الماضي", "هذا الشهر", "هذا الشهر", "الشهر القادم", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 mo. ago", "last mo.", "this mo.", "this mo.", "next mo.", "in 2 mo." ]; // prettier-ignore
        const ar = [ "قبل شهرين", "الشهر الماضي", "هذا الشهر", "هذا الشهر", "الشهر القادم", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 mo. ago", "last mo.", "this mo.", "this mo.", "next mo.", "in 2 mo." ]; // prettier-ignore
        const ar = [ "قبل شهرين", "الشهر الماضي", "هذا الشهر", "هذا الشهر", "الشهر القادم", "خلال شهرين" ]; // prettier-ignore

        runTest("month", "narrow", "auto", en, ar);
    });
});

describe("quarter", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 quarters ago", "1 quarter ago", "0 quarters ago", "in 0 quarters", "in 1 quarter", "in 2 quarters" ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "قبل ربع سنة واحد", "قبل ٠ ربع سنة", "خلال ٠ ربع سنة", "خلال ربع سنة واحد", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 qtrs. ago", "1 qtr. ago", "0 qtrs. ago", "in 0 qtrs.", "in 1 qtr.", "in 2 qtrs." ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "قبل ربع سنة واحد", "قبل ٠ ربع سنة", "خلال ٠ ربع سنة", "خلال ربع سنة واحد", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 qtrs. ago", "1 qtr. ago", "0 qtrs. ago", "in 0 qtrs.", "in 1 qtr.", "in 2 qtrs." ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "قبل ربع سنة واحد", "قبل ٠ ربع سنة", "خلال ٠ ربع سنة", "خلال ربع سنة واحد", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 quarters ago", "last quarter", "this quarter", "this quarter", "next quarter", "in 2 quarters" ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "الربع الأخير", "هذا الربع", "هذا الربع", "الربع القادم", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 qtrs. ago", "last qtr.", "this qtr.", "this qtr.", "next qtr.", "in 2 qtrs." ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "الربع الأخير", "هذا الربع", "هذا الربع", "الربع القادم", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 qtrs. ago", "last qtr.", "this qtr.", "this qtr.", "next qtr.", "in 2 qtrs." ]; // prettier-ignore
        const ar = [ "قبل ربعي سنة", "الربع الأخير", "هذا الربع", "هذا الربع", "الربع القادم", "خلال ربعي سنة" ]; // prettier-ignore

        runTest("quarter", "narrow", "auto", en, ar);
    });
});

describe("year", () => {
    test("style=long, numeric=always", () => {
        const en = [ "2 years ago", "1 year ago", "0 years ago", "in 0 years", "in 1 year", "in 2 years" ]; // prettier-ignore
        const ar = [ "قبل سنتين", "قبل سنة واحدة", "قبل ٠ سنة", "خلال ٠ سنة", "خلال سنة واحدة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "long", "always", en, ar);
    });

    test("style=short, numeric=always", () => {
        const en = [ "2 yr. ago", "1 yr. ago", "0 yr. ago", "in 0 yr.", "in 1 yr.", "in 2 yr." ]; // prettier-ignore
        const ar = [ "قبل سنتين", "قبل سنة واحدة", "قبل ٠ سنة", "خلال ٠ سنة", "خلال سنة واحدة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "short", "always", en, ar);
    });

    test("style=narrow, numeric=always", () => {
        const en = [ "2 yr. ago", "1 yr. ago", "0 yr. ago", "in 0 yr.", "in 1 yr.", "in 2 yr." ]; // prettier-ignore
        const ar = [ "قبل سنتين", "قبل سنة واحدة", "قبل ٠ سنة", "خلال ٠ سنة", "خلال سنة واحدة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "narrow", "always", en, ar);
    });

    test("style=long, numeric=auto", () => {
        const en = [ "2 years ago", "last year", "this year", "this year", "next year", "in 2 years" ]; // prettier-ignore
        const ar = [ "قبل سنتين", "السنة الماضية", "السنة الحالية", "السنة الحالية", "السنة القادمة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "long", "auto", en, ar);
    });

    test("style=short, numeric=auto", () => {
        const en = [ "2 yr. ago", "last yr.", "this yr.", "this yr.", "next yr.", "in 2 yr." ]; // prettier-ignore
        const ar = [ "قبل سنتين", "السنة الماضية", "السنة الحالية", "السنة الحالية", "السنة القادمة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "short", "auto", en, ar);
    });

    test("style=narrow, numeric=auto", () => {
        const en = [ "2 yr. ago", "last yr.", "this yr.", "this yr.", "next yr.", "in 2 yr." ]; // prettier-ignore
        const ar = [ "قبل سنتين", "السنة الماضية", "السنة الحالية", "السنة الحالية", "السنة القادمة", "خلال سنتين" ]; // prettier-ignore

        runTest("year", "narrow", "auto", en, ar);
    });
});
