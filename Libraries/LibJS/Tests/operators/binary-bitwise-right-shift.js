load("test-common.js");

try {
    assert((0 >> 0) === 0);
    assert((0 >> 1) === 0);
    assert((0 >> 2) === 0);
    assert((0 >> 3) === 0);
    assert((0 >> 4) === 0);
    assert((0 >> 5) === 0);

    assert((1 >> 0) === 1);
    assert((1 >> 1) === 0);
    assert((1 >> 2) === 0);
    assert((1 >> 3) === 0);
    assert((1 >> 4) === 0);
    assert((1 >> 5) === 0);

    assert((5 >> 0) === 5);
    assert((5 >> 1) === 2);
    assert((5 >> 2) === 1);
    assert((5 >> 3) === 0);
    assert((5 >> 4) === 0);
    assert((5 >> 5) === 0);

    assert((42 >> 0) === 42);
    assert((42 >> 1) === 21);
    assert((42 >> 2) === 10);
    assert((42 >> 3) === 5);
    assert((42 >> 4) === 2);
    assert((42 >> 5) === 1);

    assert((-1 >> 0) === -1);
    assert((-1 >> 1) === -1);
    assert((-1 >> 2) === -1);
    assert((-1 >> 3) === -1);
    assert((-1 >> 4) === -1);
    assert((-1 >> 5) === -1);

    assert((-5 >> 0) === -5);
    assert((-5 >> 1) === -3);
    assert((-5 >> 2) === -2);
    assert((-5 >> 3) === -1);
    assert((-5 >> 4) === -1);
    assert((-5 >> 5) === -1);

    var x = 67;
    var y = 4;
    assert(("42" >> 3) === 5);
    assert((x >> y) === 4);
    assert((x >> [[[[5]]]]) === 2);
    assert((undefined >> y) === 0);
    assert(("a" >> "b") === 0);
    assert((null >> null) === 0);
    assert((undefined >> undefined) === 0);
    assert((NaN >> NaN) === 0);
    assert((6 >> NaN) === 6);
    assert((Infinity >> Infinity) === 0);
    assert((-Infinity >> Infinity) === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
