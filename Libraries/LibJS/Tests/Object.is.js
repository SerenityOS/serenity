load("test-common.js");

try {
    assert(Object.is.length === 2);

    var a = [1, 2, 3];
    var o = {foo: "bar"};

    assert(Object.is("", "") === true);
    assert(Object.is("foo", "foo") === true);
    assert(Object.is(0, 0) === true);
    assert(Object.is(+0, +0) === true);
    assert(Object.is(-0, -0) === true);
    assert(Object.is(1.23, 1.23) === true);
    assert(Object.is(42, 42) === true);
    assert(Object.is(NaN, NaN) === true);
    assert(Object.is(Infinity, Infinity) === true);
    assert(Object.is(+Infinity, +Infinity) === true);
    assert(Object.is(-Infinity, -Infinity) === true);
    assert(Object.is(true, true) === true);
    assert(Object.is(false, false) === true);
    assert(Object.is(null, null) === true);
    assert(Object.is(undefined, undefined) === true);
    assert(Object.is(undefined) === true);
    assert(Object.is() === true);
    assert(Object.is(a, a) === true);
    assert(Object.is(o, o) === true);

    assert(Object.is("test") === false);
    assert(Object.is("foo", "bar") === false);
    assert(Object.is(1, "1") === false);
    assert(Object.is(+0, -0) === false);
    assert(Object.is(-0, +0) === false);
    assert(Object.is(42, 24) === false);
    assert(Object.is(Infinity, -Infinity) === false);
    assert(Object.is(-Infinity, +Infinity) === false);
    assert(Object.is(true, false) === false);
    assert(Object.is(false, true) === false);
    assert(Object.is(undefined, null) === false);
    assert(Object.is(null, undefined) === false);
    assert(Object.is([], []) === false);
    assert(Object.is(a, [1, 2, 3]) === false);
    assert(Object.is([1, 2, 3], a) === false);
    assert(Object.is({}, {}) === false);
    assert(Object.is(o, {foo: "bar"}) === false);
    assert(Object.is({foo: "bar"}, o) === false);
    assert(Object.is(a, o) === false);
    assert(Object.is(o, a) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
