test("basic functionality", () => {
    expect(String.prototype.toLocaleLowerCase).toHaveLength(0);

    expect("ω".toLocaleLowerCase()).toBe("ω");
    expect("Ω".toLocaleLowerCase()).toBe("ω");
    expect("😀".toLocaleLowerCase()).toBe("😀");

    expect("foo".toLocaleLowerCase()).toBe("foo");
    expect("Foo".toLocaleLowerCase()).toBe("foo");
    expect("FOO".toLocaleLowerCase()).toBe("foo");

    expect(("b" + "a" + +"a" + "a").toLocaleLowerCase()).toBe("banana");
});

test("special case folding", () => {
    expect("\u00DF".toLocaleLowerCase()).toBe("\u00DF");
    expect("\u0130".toLocaleLowerCase()).toBe("\u0069\u0307");
    expect("\uFB00".toLocaleLowerCase()).toBe("\uFB00");
    expect("\uFB01".toLocaleLowerCase()).toBe("\uFB01");
    expect("\uFB02".toLocaleLowerCase()).toBe("\uFB02");
    expect("\uFB03".toLocaleLowerCase()).toBe("\uFB03");
    expect("\uFB04".toLocaleLowerCase()).toBe("\uFB04");
    expect("\uFB05".toLocaleLowerCase()).toBe("\uFB05");
    expect("\uFB06".toLocaleLowerCase()).toBe("\uFB06");
    expect("\u1FB7".toLocaleLowerCase()).toBe("\u1FB7");
    expect("\u1FC7".toLocaleLowerCase()).toBe("\u1FC7");
    expect("\u1FF7".toLocaleLowerCase()).toBe("\u1FF7");
});
