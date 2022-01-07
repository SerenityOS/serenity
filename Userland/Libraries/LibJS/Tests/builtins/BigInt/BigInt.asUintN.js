describe("correct behavior", () => {
    test("length is 2", () => {
        expect(BigInt.asUintN).toHaveLength(2);
    });

    test("basic functionality", () => {
        expect(BigInt.asUintN(0, 1n)).toBe(0n);
        expect(BigInt.asUintN(0, 2n)).toBe(0n);
        expect(BigInt.asUintN(4, 15n)).toBe(15n);
        expect(BigInt.asUintN(4, 16n)).toBe(0n);
        expect(BigInt.asUintN(4, 17n)).toBe(1n);
        expect(BigInt.asUintN(32, 2n ** 32n)).toBe(0n);
        expect(BigInt.asUintN(64, 2n ** 64n)).toBe(0n);
        expect(BigInt.asUintN(10, 0xf00ba5n)).toBe(933n);
        expect(BigInt.asUintN(11, 0xf00ba5n)).toBe(933n);
        expect(BigInt.asUintN(12, 0xf00ba5n)).toBe(2981n);
        expect(BigInt.asUintN(21, 0xf00ba5n)).toBe(1051557n);
        expect(BigInt.asUintN(22, 0xf00ba5n)).toBe(3148709n);
        expect(BigInt.asUintN(23, 0xf00ba5n)).toBe(7343013n);
        expect(BigInt.asUintN(24, 0xf00ba5n)).toBe(15731621n);
        expect(BigInt.asUintN(10, -10n)).toBe(1014n);
        expect(BigInt.asUintN(11, -10n)).toBe(2038n);
        expect(BigInt.asUintN(12, -10n)).toBe(4086n);
        expect(BigInt.asUintN(222, -10n)).toBe(
            6739986666787659948666753771754907668409286105635143120275902562294n
        );
    });

    // FIXME: This is skipped because the modulo of a negative BigInt and a number which should result in 0 actually
    //        results in -0. This trips up the logic in modulo() which sees that it's negative and then adds `bits` to
    //        the result, producing the incorrect result. For example, BigInt.asUintN(0, -2n) results in 1n, as
    //        (-2n).divided_by(1n).remainder results in -0, so it adds 1n to the result because of the
    //        `if (result.is_negative())` block.
    test.skip("negative BigInts that result in 0", () => {
        expect(BigInt.asUintN(0, -2n)).toBe(0n);
        expect(BigInt.asUintN(0, -1n)).toBe(0n);
        expect(BigInt.asUintN(2, -4n)).toBe(0n);
        expect(BigInt.asUintN(32, -4294967296n)).toBe(0n);
    });
});

describe("errors", () => {
    test("bits is negative", () => {
        expect(() => {
            BigInt.asUintN(-1, 2n);
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");

        expect(() => {
            BigInt.asUintN(-2, 2n);
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    });

    test("bigint is not a BigInt", () => {
        expect(() => {
            BigInt.asUintN(0, {});
        }).toThrowWithMessage(SyntaxError, "Invalid value for BigInt: [object Object]");

        expect(() => {
            BigInt.asUintN(0, 2);
        }).toThrowWithMessage(TypeError, "Cannot convert number to BigInt");
    });

    test("converts bits before bigint", () => {
        expect(() => {
            BigInt.asUintN(-1, {});
        }).toThrowWithMessage(RangeError, "Index must be a positive integer");
    });
});
