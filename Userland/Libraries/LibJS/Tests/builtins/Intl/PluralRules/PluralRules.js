describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Intl.PluralRules();
        }).toThrowWithMessage(TypeError, "Intl.PluralRules constructor must be called with 'new'");
    });

    test("options is an invalid type", () => {
        expect(() => {
            new Intl.PluralRules("en", null);
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
    });

    test("localeMatcher option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { localeMatcher: "hello!" });
        }).toThrowWithMessage(RangeError, "hello! is not a valid value for option localeMatcher");
    });

    test("type option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { type: "hello!" });
        }).toThrowWithMessage(RangeError, "hello! is not a valid value for option type");
    });

    test("minimumIntegerDigits option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { minimumIntegerDigits: 1n });
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");

        expect(() => {
            new Intl.PluralRules("en", { minimumIntegerDigits: "hello!" });
        }).toThrowWithMessage(RangeError, "Value NaN is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { minimumIntegerDigits: 0 });
        }).toThrowWithMessage(RangeError, "Value 0 is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { minimumIntegerDigits: 22 });
        }).toThrowWithMessage(RangeError, "Value 22 is NaN or is not between 1 and 21");
    });

    test("minimumFractionDigits option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { minimumFractionDigits: 1n });
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");

        expect(() => {
            new Intl.PluralRules("en", { minimumFractionDigits: "hello!" });
        }).toThrowWithMessage(RangeError, "Value NaN is NaN or is not between 0 and 20");

        expect(() => {
            new Intl.PluralRules("en", { minimumFractionDigits: -1 });
        }).toThrowWithMessage(RangeError, "Value -1 is NaN or is not between 0 and 20");

        expect(() => {
            new Intl.PluralRules("en", { minimumFractionDigits: 21 });
        }).toThrowWithMessage(RangeError, "Value 21 is NaN or is not between 0 and 20");
    });

    test("maximumFractionDigits option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { maximumFractionDigits: 1n });
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");

        expect(() => {
            new Intl.PluralRules("en", { maximumFractionDigits: "hello!" });
        }).toThrowWithMessage(RangeError, "Value NaN is NaN or is not between 0 and 20");

        expect(() => {
            new Intl.PluralRules("en", { maximumFractionDigits: -1 });
        }).toThrowWithMessage(RangeError, "Value -1 is NaN or is not between 0 and 20");

        expect(() => {
            new Intl.PluralRules("en", { maximumFractionDigits: 21 });
        }).toThrowWithMessage(RangeError, "Value 21 is NaN or is not between 0 and 20");

        expect(() => {
            new Intl.PluralRules("en", { minimumFractionDigits: 10, maximumFractionDigits: 5 });
        }).toThrowWithMessage(RangeError, "Minimum value 10 is larger than maximum value 5");
    });

    test("minimumSignificantDigits option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { minimumSignificantDigits: 1n });
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");

        expect(() => {
            new Intl.PluralRules("en", { minimumSignificantDigits: "hello!" });
        }).toThrowWithMessage(RangeError, "Value NaN is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { minimumSignificantDigits: 0 });
        }).toThrowWithMessage(RangeError, "Value 0 is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { minimumSignificantDigits: 22 });
        }).toThrowWithMessage(RangeError, "Value 22 is NaN or is not between 1 and 21");
    });

    test("maximumSignificantDigits option is invalid ", () => {
        expect(() => {
            new Intl.PluralRules("en", { maximumSignificantDigits: 1n });
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");

        expect(() => {
            new Intl.PluralRules("en", { maximumSignificantDigits: "hello!" });
        }).toThrowWithMessage(RangeError, "Value NaN is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { maximumSignificantDigits: 0 });
        }).toThrowWithMessage(RangeError, "Value 0 is NaN or is not between 1 and 21");

        expect(() => {
            new Intl.PluralRules("en", { maximumSignificantDigits: 22 });
        }).toThrowWithMessage(RangeError, "Value 22 is NaN or is not between 1 and 21");
    });
});

describe("normal behavior", () => {
    test("length is 0", () => {
        expect(Intl.PluralRules).toHaveLength(0);
    });

    test("all valid localeMatcher options", () => {
        ["lookup", "best fit"].forEach(localeMatcher => {
            expect(() => {
                new Intl.PluralRules("en", { localeMatcher: localeMatcher });
            }).not.toThrow();
        });
    });

    test("all valid type options", () => {
        ["cardinal", "ordinal"].forEach(type => {
            expect(() => {
                new Intl.PluralRules("en", { type: type });
            }).not.toThrow();
        });
    });

    test("all valid minimumIntegerDigits options", () => {
        for (let i = 1; i <= 21; ++i) {
            expect(() => {
                new Intl.PluralRules("en", { minimumIntegerDigits: i });
            }).not.toThrow();
        }
    });

    test("all valid minimumFractionDigits options", () => {
        for (let i = 0; i <= 20; ++i) {
            expect(() => {
                new Intl.PluralRules("en", { minimumFractionDigits: i });
            }).not.toThrow();
        }
    });

    test("all valid maximumFractionDigits options", () => {
        for (let i = 0; i <= 20; ++i) {
            expect(() => {
                new Intl.PluralRules("en", { maximumFractionDigits: i });
            }).not.toThrow();
        }
    });

    test("all valid minimumSignificantDigits options", () => {
        for (let i = 1; i <= 21; ++i) {
            expect(() => {
                new Intl.PluralRules("en", { minimumSignificantDigits: i });
            }).not.toThrow();
        }
    });

    test("all valid maximumSignificantDigits options", () => {
        for (let i = 1; i <= 21; ++i) {
            expect(() => {
                new Intl.PluralRules("en", { maximumSignificantDigits: i });
            }).not.toThrow();
        }
    });
});
