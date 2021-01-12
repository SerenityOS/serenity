describe("correct behavior", () => {
    test("basic functionality", () => {
        expect(BigInt).toHaveLength(1);
        expect(BigInt.name).toBe("BigInt");
    });

    test("constructor with numbers", () => {
        expect(BigInt(0)).toBe(0n);
        expect(BigInt(1)).toBe(1n);
        expect(BigInt(+1)).toBe(1n);
        expect(BigInt(-1)).toBe(-1n);
        expect(BigInt(123n)).toBe(123n);
    });

    test("constructor with strings", () => {
        expect(BigInt("")).toBe(0n);
        expect(BigInt("0")).toBe(0n);
        expect(BigInt("1")).toBe(1n);
        expect(BigInt("+1")).toBe(1n);
        expect(BigInt("-1")).toBe(-1n);
        expect(BigInt("-1")).toBe(-1n);
        expect(BigInt("42")).toBe(42n);
        expect(BigInt("  \n  00100  \n  ")).toBe(100n);
        expect(BigInt("3323214327642987348732109829832143298746432437532197321")).toBe(
            3323214327642987348732109829832143298746432437532197321n
        );
    });

    test("constructor with objects", () => {
        expect(BigInt([])).toBe(0n);
    });
});

describe("errors", () => {
    test('cannot be constructed with "new"', () => {
        expect(() => {
            new BigInt();
        }).toThrowWithMessage(TypeError, "BigInt is not a constructor");
    });

    test("invalid arguments", () => {
        expect(() => {
            BigInt(null);
        }).toThrowWithMessage(TypeError, "Cannot convert null to BigInt");

        expect(() => {
            BigInt(undefined);
        }).toThrowWithMessage(TypeError, "Cannot convert undefined to BigInt");

        expect(() => {
            BigInt(Symbol());
        }).toThrowWithMessage(TypeError, "Cannot convert symbol to BigInt");

        ["foo", "123n", "1+1", {}, function () {}].forEach(value => {
            expect(() => {
                BigInt(value);
            }).toThrowWithMessage(SyntaxError, `Invalid value for BigInt: ${value}`);
        });
    });

    test("invalid numeric arguments", () => {
        [1.23, Infinity, -Infinity, NaN].forEach(value => {
            expect(() => {
                BigInt(value);
            }).toThrowWithMessage(RangeError, "BigInt argument must be an integer");
        });
    });
});
