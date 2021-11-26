test("regular exponentiation", () => {
    expect(2 ** 0).toBe(1);
    expect(2 ** 1).toBe(2);
    expect(2 ** 2).toBe(4);
    expect(2 ** 3).toBe(8);
    expect(3 ** 2).toBe(9);
    expect(0 ** 0).toBe(1);
    expect(2 ** (3 ** 2)).toBe(512);
    expect(2 ** (3 ** 2)).toBe(512);
    expect((2 ** 3) ** 2).toBe(64);
});

test("exponentiation with negatives", () => {
    expect(2 ** -3).toBe(0.125);
    expect((-2) ** 3).toBe(-8);

    // FIXME: This should fail :)
    // expect("-2 ** 3").not.toEval();
});

test("exponentiation with non-numeric primitives", () => {
    expect("2" ** "3").toBe(8);
    expect("" ** []).toBe(1);
    expect([] ** null).toBe(1);
    expect(null ** null).toBe(1);
    expect(undefined ** null).toBe(1);
});

test("exponentiation that produces NaN", () => {
    expect(NaN ** 2).toBeNaN();
    expect(2 ** NaN).toBeNaN();
    expect(undefined ** 2).toBeNaN();
    expect(2 ** undefined).toBeNaN();
    expect(null ** undefined).toBeNaN();
    expect(2 ** "foo").toBeNaN();
    expect("foo" ** 2).toBeNaN();
});
