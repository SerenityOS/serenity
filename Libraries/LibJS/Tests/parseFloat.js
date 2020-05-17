load("test-common.js");

try {
    const NUMBER_TEST_CASES = [
        [0, 0],
        [1, 1],
        [.23, 0.23],
        [1.23, 1.23],
        [0.0123E+2, 1.23],
        [1.23e4, 12300],
        [Infinity, Infinity]
    ];

    NUMBER_TEST_CASES.forEach(test => {
        const value = test[0];
        const result = test[1];
        assert(parseFloat(value) === result);
        assert(parseFloat(+value) === result);
        assert(parseFloat(-value) === -result);
    });

    const STRING_TEST_CASES = [
        ["0", 0],
        ["1", 1],
        [".23", 0.23],
        ["1.23", 1.23],
        ["0.0123E+2", 1.23],
        ["1.23e4", 12300],
        ["Infinity", Infinity]
    ];

    STRING_TEST_CASES.forEach(test => {
        const value = test[0];
        const result = test[1];
        assert(parseFloat(value) === result);
        assert(parseFloat(`+${value}`) === result);
        assert(parseFloat(`-${value}`) === -result);
        assert(parseFloat(`${value}foo`) === result);
        assert(parseFloat(`+${value}foo`) === result);
        assert(parseFloat(`-${value}foo`) === -result);
        assert(parseFloat(`   \n  \t ${value} \v  foo   `) === result);
        assert(parseFloat(`   \r -${value} \f \n\n  foo   `) === -result);
        assert(parseFloat({ toString: () => value }) === result);
    });

    const NAN_TEST_CASES = [
        "",
        [],
        [],
        true,
        false,
        null,
        undefined,
        NaN,
        "foo123",
        "foo+123",
        "fooInfinity",
        "foo+Infinity"
    ];

    assert(isNaN(parseFloat()));
    assert(isNaN(parseFloat("", 123, Infinity)));

    NAN_TEST_CASES.forEach(value => {
        assert(isNaN(parseFloat(value)));
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e)
}
