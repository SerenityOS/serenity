test("basic functionality", () => {
    expect(String.prototype.toLowerCase).toHaveLength(0);

    expect("foo".toLowerCase()).toBe("foo");
    expect("Foo".toLowerCase()).toBe("foo");
    expect("FOO".toLowerCase()).toBe("foo");

    expect(("b" + "a" + +"a" + "a").toLowerCase()).toBe("banana");
});
