test("basic functionality", () => {
    expect(String.prototype.toString).toHaveLength(0);

    expect("".toString()).toBe("");
    expect("hello friends".toString()).toBe("hello friends");
});
