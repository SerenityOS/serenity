describe("correct behavior", () => {
    test("typeof bigint", () => {
        expect(typeof 1n).toBe("bigint");
    });

    test("bigint string coercion", () => {
        expect("" + 123n).toBe("123");
    });

    test("arithmetic operators", () => {
        let bigint = 123n;
        expect(-bigint).toBe(-123n);

        expect(12n + 34n).toBe(46n);
        expect(12n - 34n).toBe(-22n);
        expect(8n * 12n).toBe(96n);
        expect(123n / 10n).toBe(12n);
        expect(2n ** 3n).toBe(8n);
        expect(5n % 3n).toBe(2n);
        expect(
            45977665298704210987n +
                (714320987142450987412098743217984576n / 4598741987421098765327980n) * 987498743n
        ).toBe(199365500239020623962n);
    });

    test("bitwise operators", () => {
        expect(12n & 5n).toBe(4n);
        expect(1n | 2n).toBe(3n);
        expect(5n ^ 3n).toBe(6n);
        expect(~1n).toBe(-2n);
    });

    test("increment operators", () => {
        let bigint = 1n;
        expect(bigint++).toBe(1n);
        expect(bigint).toBe(2n);
        expect(bigint--).toBe(2n);
        expect(bigint).toBe(1n);
        expect(++bigint).toBe(2n);
        expect(bigint).toBe(2n);
        expect(--bigint).toBe(1n);
        expect(bigint).toBe(1n);
    });

    test("weak equality operators", () => {
        expect(1n == 1n).toBeTrue();
        expect(1n == 1).toBeTrue();
        expect(1 == 1n).toBeTrue();
        expect(1n == 1.23).toBeFalse();
        expect(1.23 == 1n).toBeFalse();

        expect(1n != 1n).toBeFalse();
        expect(1n != 1).toBeFalse();
        expect(1 != 1n).toBeFalse();
        expect(1n != 1.23).toBeTrue();
        expect(1.23 != 1n).toBeTrue();
    });

    test("strong equality operators", () => {
        expect(1n === 1n).toBeTrue();
        expect(1n === 1).toBeFalse();
        expect(1 === 1n).toBeFalse();
        expect(1n === 1.23).toBeFalse();
        expect(1.23 === 1n).toBeFalse();

        expect(1n !== 1n).toBeFalse();
        expect(1n !== 1).toBeTrue();
        expect(1 !== 1n).toBeTrue();
        expect(1n !== 1.23).toBeTrue();
        expect(1.23 !== 1n).toBeTrue();
    });
});

describe("errors", () => {
    test("conversion to number", () => {
        expect(() => {
            +123n;
        }).toThrowWithMessage(TypeError, "Cannot convert BigInt to number");
    });
});
