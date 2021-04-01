test("invariants", () => {
    expect(String.prototype.replace).toHaveLength(2);
});

test("error cases", () => {
    [null, undefined].forEach(value => {
        expect(() => {
            value.replace("", "");
        }).toThrow(TypeError);
    });
});

test("basic string replacement", () => {
    expect("".replace("", "")).toBe("");
    expect("".replace("a", "")).toBe("");
    expect("".replace("", "a")).toBe("a");

    expect("a".replace("a", "")).toBe("");
    expect("a".replace("a", "b")).toBe("b");
    expect("aa".replace("a", "b")).toBe("ba");
    expect("ca".replace("a", "b")).toBe("cb");
});

test("convertible string replacement", () => {
    expect("123".replace(2, "x")).toBe("1x3");
    expect("123".replace("2", 4)).toBe("143");
    expect("123".replace(2, 4)).toBe("143");
});

test("functional string replacement", () => {
    expect(
        "a".replace("a", function () {
            return "b";
        })
    ).toBe("b");
    expect("a".replace("a", () => "b")).toBe("b");

    expect(
        "abc".replace("b", (search, position, string) => {
            expect(search).toBe("b");
            expect(position).toBe(1);
            expect(string).toBe("abc");
            return "x";
        })
    ).toBe("axc");
});

test("basic regex replacement", () => {
    expect("".replace(/a/, "")).toBe("");
    expect("a".replace(/a/, "")).toBe("");

    expect("abc123def".replace(/\D/, "*")).toBe("*bc123def");
    expect("123abc456".replace(/\D/, "*")).toBe("123*bc456");
    expect("abc123def".replace(/\D/g, "*")).toBe("***123***");
    expect("123abc456".replace(/\D/g, "*")).toBe("123***456");
});

test("functional regex replacement", () => {
    expect(
        "a".replace(/a/, function () {
            return "b";
        })
    ).toBe("b");
    expect("a".replace(/a/, () => "b")).toBe("b");

    expect(
        "abc".replace(/\D/, (matched, position, string) => {
            expect(matched).toBe("a");
            expect(position).toBe(0);
            expect(string).toBe("abc");
            return "x";
        })
    ).toBe("xbc");

    expect(
        "abc".replace(/\D/g, (matched, position, string) => {
            expect(matched).toBe(string[position]);
            expect(position <= 2).toBeTrue();
            expect(string).toBe("abc");
            return "x";
        })
    ).toBe("xxx");

    expect(
        "abc".replace(/(\D)/g, (matched, capture1, position, string) => {
            expect(matched).toBe(string[position]);
            expect(capture1).toBe(string[position]);
            expect(position <= 2).toBeTrue();
            expect(string).toBe("abc");
            return "x";
        })
    ).toBe("xxx");

    expect(
        "abcd".replace(/(\D)b(\D)/g, (matched, capture1, capture2, position, string) => {
            expect(matched).toBe("abc");
            expect(capture1).toBe("a");
            expect(capture2).toBe("c");
            expect(position).toBe(0);
            expect(string).toBe("abcd");
            return "x";
        })
    ).toBe("xd");
});
