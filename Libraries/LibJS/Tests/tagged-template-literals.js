load("test-common.js");

try {
    assertThrowsError(() => {
        foo`bar${baz}`;
    }, {
        error: ReferenceError,
        message: "'foo' not known"
    });

    assertThrowsError(() => {
        function foo() { }
        foo`bar${baz}`;
    }, {
        error: ReferenceError,
        message: "'baz' not known"
    });

    assertThrowsError(() => {
        undefined``````;
    }, {
        error: TypeError,
        message: "undefined is not a function"
    });

    function test1(strings) {
        assert(strings instanceof Array);
        assert(strings.length === 1);
        assert(strings[0] === "");
        return 42;
    }
    assert(test1`` === 42);

    function test2(s) {
        return function (strings) {
            assert(strings instanceof Array);
            assert(strings.length === 1);
            assert(strings[0] === "bar");
            return s + strings[0];
        }
    }
    assert(test2("foo")`bar` === "foobar");

    var test3 = {
        foo(strings, p1) {
            assert(strings instanceof Array);
            assert(strings.length === 2);
            assert(strings[0] === "");
            assert(strings[1] === "");
            assert(p1 === "bar");
        }
    };
    test3.foo`${"bar"}`;

    function test4(strings, p1) {
        assert(strings instanceof Array);
        assert(strings.length === 2);
        assert(strings[0] === "foo");
        assert(strings[1] === "");
        assert(p1 === 42);
    }
    var bar = 42;
    test4`foo${bar}`;

    function test5(strings, p1, p2) {
        assert(strings instanceof Array);
        assert(strings.length === 3);
        assert(strings[0] === "foo");
        assert(strings[1] === "baz");
        assert(strings[2] === "");
        assert(p1 === 42);
        assert(p2 === "qux");
        return (strings, value) => `${value}${strings[0]}`;
    }
    var bar = 42;
    assert(test5`foo${bar}baz${"qux"}``test${123}` === "123test");

    function review(strings, name, rating) {
        return `${strings[0]}**${name}**${strings[1]}_${rating}_${strings[2]}`;
    }
    var name = "SerenityOS";
    var rating = "great";
    assert(review`${name} is a ${rating} project!` === "**SerenityOS** is a _great_ project!");

    const getTemplateObject = (...rest) => rest;
    const getRawTemplateStrings = arr => arr.raw;

    let o = getTemplateObject`foo\nbar`;
    assert(Object.getOwnPropertyNames(o[0]).includes('raw'));

    let raw = getRawTemplateStrings`foo${1 + 3}\nbar`;
    assert(!Object.getOwnPropertyNames(raw).includes('raw'));
    assert(raw.length === 2);
    assert(raw[0] === 'foo');
    assert(raw[1].length === 5 && raw[1] === '\\nbar');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
