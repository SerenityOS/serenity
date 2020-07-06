let x;

test("basic functionality", () => {
    x = 1;
    expect((x = 2)).toBe(2);
    expect(x).toBe(2);

    x = 1;
    expect((x += 2)).toBe(3);
    expect(x).toBe(3);

    x = 3;
    expect((x -= 2)).toBe(1);
    expect(x).toBe(1);

    x = 3;
    expect((x *= 2)).toBe(6);
    expect(x).toBe(6);

    x = 6;
    expect((x /= 2)).toBe(3);
    expect(x).toBe(3);

    x = 6;
    expect((x %= 4)).toBe(2);
    expect(x).toBe(2);

    x = 2;
    expect((x **= 3)).toBe(8);
    expect(x).toBe(8);

    x = 3;
    expect((x &= 2)).toBe(2);
    expect(x).toBe(2);

    x = 3;
    expect((x |= 4)).toBe(7);
    expect(x).toBe(7);

    x = 6;
    expect((x ^= 2)).toBe(4);
    expect(x).toBe(4);

    x = 2;
    expect((x <<= 2)).toBe(8);
    expect(x).toBe(8);

    x = 8;
    expect((x >>= 2)).toBe(2);
    expect(x).toBe(2);

    x = -(2 ** 32 - 10);
    expect((x >>>= 2)).toBe(2);
    expect(x).toBe(2);
});
