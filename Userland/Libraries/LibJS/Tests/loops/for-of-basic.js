describe("correct behavior", () => {
    test("iterate through array", () => {
        const a = [];
        for (const num of [1, 2, 3]) {
            a.push(num);
        }
        expect(a).toEqual([1, 2, 3]);
    });

    test("iterate through string", () => {
        const a = [];
        for (const char of "hello") {
            a.push(char);
        }
        expect(a).toEqual(["h", "e", "l", "l", "o"]);
    });

    test("iterate through string object", () => {
        const a = [];
        for (const char of new String("hello")) {
            a.push(char);
        }
        expect(a).toEqual(["h", "e", "l", "l", "o"]);
    });

    test("use already-declared variable", () => {
        var char;
        for (char of "abc");
        expect(char).toBe("c");
    });

    test("respects custom Symbol.iterator method", () => {
        const o = {
            [Symbol.iterator]() {
                return {
                    i: 0,
                    next() {
                        if (this.i++ == 3) {
                            return { done: true };
                        }
                        return { value: this.i, done: false };
                    },
                };
            },
        };

        const a = [];
        for (const k of o) {
            a.push(k);
        }

        expect(a).toEqual([1, 2, 3]);
    });

    test("loops through custom iterator if there is an exception thrown part way through", () => {
        // This tests against the way custom iterators used to be implemented, where the values
        // were all collected at once before the for-of body was executed, instead of getting
        // the values one at a time
        const o = {
            [Symbol.iterator]() {
                return {
                    i: 0,
                    next() {
                        if (this.i++ === 3) {
                            throw new Error();
                        }
                        return { value: this.i };
                    },
                };
            },
        };

        const a = [];

        try {
            for (let k of o) {
                a.push(k);
            }
            expect().fail();
        } catch (e) {
            expect(a).toEqual([1, 2, 3]);
        }
    });
});

describe("errors", () => {
    test("right hand side is a primitive", () => {
        expect(() => {
            for (const _ of 123) {
            }
        }).toThrowWithMessage(TypeError, "123 is not iterable");
    });

    test("right hand side is an object", () => {
        expect(() => {
            for (const _ of { foo: 1, bar: 2 }) {
            }
        }).toThrowWithMessage(TypeError, "[object Object] is not iterable");
    });
});
