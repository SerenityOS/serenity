describe("correct behavior", () => {
    test("length", () => {
        expect(JSON.stringify).toHaveLength(3);
    });

    test("basic functionality", () => {
        [
            [5, "5"],
            [undefined, undefined],
            [null, "null"],
            [NaN, "null"],
            [-NaN, "null"],
            [Infinity, "null"],
            [-Infinity, "null"],
            [true, "true"],
            [false, "false"],
            ["test", '"test"'],
            [new Number(5), "5"],
            [new Boolean(false), "false"],
            [new String("test"), '"test"'],
            [() => {}, undefined],
            [[1, 2, "foo"], '[1,2,"foo"]'],
            [{ foo: 1, bar: "baz", qux() {} }, '{"foo":1,"bar":"baz"}'],
            [
                {
                    var1: 1,
                    var2: 2,
                    toJSON(key) {
                        let o = this;
                        o.var2 = 10;
                        return o;
                    },
                },
                '{"var1":1,"var2":10}',
            ],
        ].forEach(testCase => {
            expect(JSON.stringify(testCase[0])).toEqual(testCase[1]);
        });
    });

    test("ignores non-enumerable properties", () => {
        let o = { foo: "bar" };
        Object.defineProperty(o, "baz", { value: "qux", enumerable: false });
        expect(JSON.stringify(o)).toBe('{"foo":"bar"}');
    });
});

describe("errors", () => {
    test("cannot serialize BigInt", () => {
        expect(() => {
            JSON.stringify(5n);
        }).toThrow(TypeError, "Cannot serialize BigInt value to JSON");
    });

    test("cannot serialize circular structures", () => {
        let bad1 = {};
        bad1.foo = bad1;
        let bad2 = [];
        bad2[5] = [[[bad2]]];

        let bad3a = { foo: "bar" };
        let bad3b = [1, 2, bad3a];
        bad3a.bad = bad3b;

        [bad1, bad2, bad3a].forEach(bad => {
            expect(() => {
                JSON.stringify(bad);
            }).toThrow(TypeError, "Cannot stringify circular object");
        });
    });
});
