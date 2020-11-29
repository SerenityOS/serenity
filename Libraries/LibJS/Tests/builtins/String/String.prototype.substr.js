test("basic functionality", () => {
    expect(String.prototype.substr).toHaveLength(2);

    expect("hello friends".substr()).toBe("hello friends");
    expect("hello friends".substr(1)).toBe("ello friends");
    expect("hello friends".substr(0, 5)).toBe("hello");
    expect("hello friends".substr(5, 6)).toBe(" frien");
    expect("hello friends".substr("", 5)).toBe("hello");
    expect("hello friends".substr(3, 3)).toBe("lo ");
    expect("hello friends".substr(-1, 13)).toBe("s");
    expect("hello friends".substr(0, 50)).toBe("hello friends");
    expect("hello friends".substr(0, "5")).toBe("hello");
    expect("hello friends".substr("2", "2")).toBe("ll");
    expect("hello friends".substr(-7)).toBe("friends");
    expect("hello friends".substr(-3, -5)).toBe("");
});
