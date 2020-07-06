describe("tagged template literal errors", () => {
    test("undefined variables in template expression throw a ReferenceError", () => {
        expect(() => {
            foo`bar${baz}`;
        }).toThrowWithMessage(ReferenceError, "'foo' is not defined");

        expect(() => {
            function foo() {}
            foo`bar${baz}`;
        }).toThrowWithMessage(ReferenceError, "'baz' is not defined");
    });

    test("cannot tag a non-function", () => {
        expect(() => {
            undefined``;
        }).toThrowWithMessage(TypeError, "undefined is not a function");
    });
});

describe("tagged template literal functionality", () => {
    test("empty template tag", () => {
        function test1(strings) {
            expect(strings).toBeInstanceOf(Array);
            expect(strings).toHaveLength(1);
            expect(strings[0]).toBe("");
            return 42;
        }
        expect(test1``).toBe(42);
    });

    test("tagging a template literal", () => {
        function test2(s) {
            return function (strings) {
                expect(strings).toBeInstanceOf(Array);
                expect(strings).toHaveLength(1);
                expect(strings[0]).toBe("bar");
                return s + strings[0];
            };
        }
        expect(test2("foo")`bar`).toBe("foobar");
    });

    test("tagging an object function key", () => {
        var test3 = {
            foo(strings, p1) {
                expect(strings).toBeInstanceOf(Array);
                expect(strings).toHaveLength(2);
                expect(strings[0]).toBe("");
                expect(strings[1]).toBe("");
                expect(p1).toBe("bar");
            },
        };
        test3.foo`${"bar"}`;
    });

    test("tagging with a variable in a template expression", () => {
        function test4(strings, p1) {
            expect(strings).toBeInstanceOf(Array);
            expect(strings).toHaveLength(2);
            expect(strings[0]).toBe("foo");
            expect(strings[1]).toBe("");
            expect(p1).toBe(42);
        }
        var bar = 42;
        test4`foo${bar}`;
    });

    test("template tag result of another template tag", () => {
        function test5(strings, p1, p2) {
            expect(strings).toBeInstanceOf(Array);
            expect(strings).toHaveLength(3);
            expect(strings[0]).toBe("foo");
            expect(strings[1]).toBe("baz");
            expect(strings[2]).toBe("");
            expect(p1).toBe(42);
            expect(p2).toBe("qux");
            return (strings, value) => `${value}${strings[0]}`;
        }
        var bar = 42;
        expect(test5`foo${bar}baz${"qux"}``test${123}`).toBe("123test");
    });

    test("general test", () => {
        function review(strings, name, rating) {
            return `${strings[0]}**${name}**${strings[1]}_${rating}_${strings[2]}`;
        }
        var name = "SerenityOS";
        var rating = "great";
        expect(review`${name} is a ${rating} project!`).toBe(
            "**SerenityOS** is a _great_ project!"
        );
    });

    test("template object structure", () => {
        const getTemplateObject = (...rest) => rest;
        const getRawTemplateStrings = arr => arr.raw;

        let o = getTemplateObject`foo\nbar`;
        expect(Object.getOwnPropertyNames(o[0])).toContain("raw");

        let raw = getRawTemplateStrings`foo${1 + 3}\nbar`;
        expect(Object.getOwnPropertyNames(raw)).not.toContain("raw");
        expect(raw).toHaveLength(2);
        expect(raw[0]).toBe("foo");
        expect(raw[1]).toHaveLength(5);
        expect(raw[1]).toBe("\\nbar");
    });
});
