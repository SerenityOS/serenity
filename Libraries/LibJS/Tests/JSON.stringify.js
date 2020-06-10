load("test-common.js");

try {
    assert(JSON.stringify.length === 3);

    assertThrowsError(() => {
        JSON.stringify(5n);
    }, {
        error: TypeError,
        message: "Cannot serialize BigInt value to JSON",
    });

    const properties = [
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
                }
            },
            '{"var1":1,"var2":10}',
        ],
    ];

    properties.forEach(testCase => {
        assert(JSON.stringify(testCase[0]) === testCase[1]);
    });

    let bad1 = {};
    bad1.foo = bad1;
    let bad2 = [];
    bad2[5] = [[[bad2]]];

    let bad3a = { foo: "bar" };
    let bad3b = [1, 2, bad3a];
    bad3a.bad = bad3b;

    [bad1, bad2, bad3a].forEach(bad => assertThrowsError(() => {
        JSON.stringify(bad);
    }, {
        error: TypeError,
        message: "Cannot stringify circular object",
    }));

    let o = { foo: "bar" };
    Object.defineProperty(o, "baz", { value: "qux", enumerable: false });
    assert(JSON.stringify(o) === '{"foo":"bar"}');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
