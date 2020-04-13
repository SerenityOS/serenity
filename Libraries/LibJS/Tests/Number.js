load("test-common.js");

try {
    assert(Number.length === 1);
    assert(typeof Number() === "number");
    assert(typeof new Number() === "object");
    assert(Number() === 0);
    assert(new Number().valueOf() === 0);
    assert(Number("42") === 42);
    assert(new Number("42").valueOf() === 42);
    assert(Number(null) === 0);
    assert(new Number(null).valueOf() === 0);
    assert(Number(true) === 1);
    assert(new Number(true).valueOf() === 1);
    assert(Number("Infinity") === Infinity);
    assert(new Number("Infinity").valueOf() === Infinity);
    assert(Number("+Infinity") === Infinity);
    assert(new Number("+Infinity").valueOf() === Infinity);
    assert(Number("-Infinity") === -Infinity);
    assert(new Number("-Infinity").valueOf() === -Infinity);
    assert(isNaN(Number(undefined)));
    assert(isNaN(new Number(undefined).valueOf()));
    assert(isNaN(Number({})));
    assert(isNaN(new Number({}).valueOf()));
    assert(isNaN(Number({a: 1})));
    assert(isNaN(new Number({a: 1}).valueOf()));
    assert(isNaN(Number([1, 2, 3])));
    assert(isNaN(new Number([1, 2, 3]).valueOf()));
    assert(isNaN(Number("foo")));
    assert(isNaN(new Number("foo").valueOf()));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e.message);
}
