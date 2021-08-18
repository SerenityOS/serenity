test("basic escapes", () => {
    var foo = {};
    foo.brown = 12389;

    expect(foo.brown).toBe(12389);
    expect(foo.br\u006fwn).toBe(12389);
    expect(foo.br\u{6f}wn).toBe(12389);
    expect(foo.\u{62}\u{72}\u{6f}\u{77}\u{6e}).toBe(12389);
});

test("non-ascii escapes", () => {
    var foo = {};
    foo.𝓑𝓻𝓸𝔀𝓷 = 12389;

    expect(foo.𝓑𝓻𝓸𝔀𝓷).toBe(12389);
    expect(foo.𝓑𝓻\ud835\udcf8𝔀𝓷).toBe(12389);
    expect(foo.𝓑𝓻\u{1d4f8}𝔀𝓷).toBe(12389);
    expect(foo.\u{1d4d1}\u{1d4fb}\u{1d4f8}\u{1d500}\u{1d4f7}).toBe(12389);
});
