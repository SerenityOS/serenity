test("basic functionality", () => {
    expect(String.prototype.toLowerCase).toHaveLength(0);

    expect("ω".toLowerCase()).toBe("ω");
    expect("Ω".toLowerCase()).toBe("ω");
    expect("😀".toLowerCase()).toBe("😀");

    expect("foo".toLowerCase()).toBe("foo");
    expect("Foo".toLowerCase()).toBe("foo");
    expect("FOO".toLowerCase()).toBe("foo");

    expect(("b" + "a" + +"a" + "a").toLowerCase()).toBe("banana");
});
