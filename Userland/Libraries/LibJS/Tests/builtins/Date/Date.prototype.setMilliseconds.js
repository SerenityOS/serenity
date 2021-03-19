test("basic functionality", () => {
    let d = new Date(2000, 2, 1);

    d.setMilliseconds(600);
    expect(d.getMilliseconds()).toBe(600);
});
