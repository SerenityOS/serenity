test("basic functionality", () => {
    expect(String.prototype.toUpperCase).toHaveLength(0);

    expect("foo".toUpperCase()).toBe("FOO");
    expect("Foo".toUpperCase()).toBe("FOO");
    expect("FOO".toUpperCase()).toBe("FOO");

    expect(("b" + "a" + +"n" + "a").toUpperCase()).toBe("BANANA");
});
