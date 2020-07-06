test("basic numeric shifting", () => {
    expect(0 >> 0).toBe(0);
    expect(0 >> 1).toBe(0);
    expect(0 >> 2).toBe(0);
    expect(0 >> 3).toBe(0);
    expect(0 >> 4).toBe(0);
    expect(0 >> 5).toBe(0);

    expect(1 >> 0).toBe(1);
    expect(1 >> 1).toBe(0);
    expect(1 >> 2).toBe(0);
    expect(1 >> 3).toBe(0);
    expect(1 >> 4).toBe(0);
    expect(1 >> 5).toBe(0);

    expect(5 >> 0).toBe(5);
    expect(5 >> 1).toBe(2);
    expect(5 >> 2).toBe(1);
    expect(5 >> 3).toBe(0);
    expect(5 >> 4).toBe(0);
    expect(5 >> 5).toBe(0);

    expect(42 >> 0).toBe(42);
    expect(42 >> 1).toBe(21);
    expect(42 >> 2).toBe(10);
    expect(42 >> 3).toBe(5);
    expect(42 >> 4).toBe(2);
    expect(42 >> 5).toBe(1);
});

test("numeric shifting with negative lhs values", () => {
    expect(-1 >> 0).toBe(-1);
    expect(-1 >> 1).toBe(-1);
    expect(-1 >> 2).toBe(-1);
    expect(-1 >> 3).toBe(-1);
    expect(-1 >> 4).toBe(-1);
    expect(-1 >> 5).toBe(-1);

    expect(-5 >> 0).toBe(-5);
    expect(-5 >> 1).toBe(-3);
    expect(-5 >> 2).toBe(-2);
    expect(-5 >> 3).toBe(-1);
    expect(-5 >> 4).toBe(-1);
    expect(-5 >> 5).toBe(-1);
});

test("shifting with non-numeric values", () => {
    let x = 67;
    let y = 4;

    expect("42" >> 3).toBe(5);
    expect(x >> y).toBe(4);
    expect(x >> [[[[5]]]]).toBe(2);
    expect(undefined >> y).toBe(0);
    expect("a" >> "b").toBe(0);
    expect(null >> null).toBe(0);
    expect(undefined >> undefined).toBe(0);
    expect(NaN >> NaN).toBe(0);
    expect(6 >> NaN).toBe(6);
    expect(Infinity >> Infinity).toBe(0);
    expect(-Infinity >> Infinity).toBe(0);
});
