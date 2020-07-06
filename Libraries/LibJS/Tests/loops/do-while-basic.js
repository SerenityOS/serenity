test("basic functionality", () => {
    let number = 0;
    do {
        number++;
    } while (number < 9);
    expect(number).toBe(9);
});

test("no braces", () => {
    let number = 0;
    do number++;
    while (number < 3);
    expect(number).toBe(3);
});
