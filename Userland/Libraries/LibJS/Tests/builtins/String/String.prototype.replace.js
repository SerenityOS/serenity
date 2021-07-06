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

test("replacement with substitution", () => {
    expect("abc".replace("b", "$")).toBe("a$c");
    expect("abc".replace("b", "$.")).toBe("a$.c");

    expect("abc".replace("b", "$$")).toBe("a$c");
    expect("abc".replace("b", ">$$<")).toBe("a>$<c");
    expect("abc".replace("b", "$$$$")).toBe("a$$c");

    expect("abc".replace("b", "$&")).toBe("abc");
    expect("a123c".replace(/\d+/, "$&")).toBe("a123c");

    expect("abc".replace("b", "$`")).toBe("aac");
    expect("aabc".replace("b", "$`")).toBe("aaaac");
    expect("a123c".replace(/\d+/, "$`")).toBe("aac");

    expect("abc".replace("b", "$'")).toBe("acc");
    expect("abcc".replace("b", "$'")).toBe("acccc");
    expect("a123c".replace(/\d+/, "$'")).toBe("acc");

    expect("abc".replace("b", "$0")).toBe("a$0c");
    expect("abc".replace("b", "$99")).toBe("a$99c");
    expect("abc".replace("b", "$100")).toBe("a$100c");
    expect("abc".replace(/(a)b(c)/, "$0")).toBe("$0");
    expect("abc".replace(/(a)b(c)/, "$1")).toBe("a");
    expect("abc".replace(/(a)b(c)/, "$2")).toBe("c");
    expect("abc".replace(/(a)b(c)/, "$3")).toBe("$3");
    expect("abc".replace(/(a)b(c)/, "$2b$1")).toBe("cba");

    expect("abc".replace("b", "$<val>")).toBe("a$<val>c");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<")).toBe("$<");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<not_terminated")).toBe("$<not_terminated");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<not_found>")).toBe("");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<val1>")).toBe("a");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<val2>")).toBe("c");
    expect("abc".replace(/(?<val1>a)b(?<val2>c)/, "$<val2>b$<val1>")).toBe("cba");

    expect(/(?<𝒜>b)/u[Symbol.replace]("abc", "d$<𝒜>$`")).toBe("adbac");
    expect(/(?<$𐒤>b)/gu[Symbol.replace]("abc", "$'$<$𐒤>d")).toBe("acbdc");
});

test("replacement with substitution and 'groups' coerced to an object", () => {
    var r = /./;
    var coercibleValue = {
        length: 1,
        0: "b",
        index: 1,
        groups: "123",
    };

    r.exec = function () {
        return coercibleValue;
    };

    expect(r[Symbol.replace]("ab", "[$<length>]")).toBe("a[3]");
});

test("replacement value is evaluated before searching the source string", () => {
    var calls = 0;
    var replaceValue = {
        toString: function () {
            calls += 1;
            return "b";
        },
    };

    var newString = "".replace("a", replaceValue);
    expect(newString).toBe("");
    expect(calls).toBe(1);

    newString = "".replace(/a/g, replaceValue);
    expect(newString).toBe("");
    expect(calls).toBe(2);
});
