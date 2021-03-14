test("basic functionality", () => {
    expect(String.prototype.match).toHaveLength(1);

    expect("hello friends".match(/hello/)).not.toBeNull();
    expect("hello friends".match(/enemies/)).toBeNull();
});
