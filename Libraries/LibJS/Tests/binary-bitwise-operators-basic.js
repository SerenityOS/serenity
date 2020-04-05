try {
    assert((0 | 0) === 0);
    assert((0 | 1) === 1);
    assert((0 | 2) === 2);
    assert((0 | 3) === 3);
    assert((0 | 4) === 4);
    assert((0 | 5) === 5);

    assert((1 | 0) === 1);
    assert((1 | 1) === 1);
    assert((1 | 2) === 3);
    assert((1 | 3) === 3);
    assert((1 | 4) === 5);
    assert((1 | 5) === 5);

    assert((2 | 0) === 2);
    assert((2 | 1) === 3);
    assert((2 | 2) === 2);
    assert((2 | 3) === 3);
    assert((2 | 4) === 6);
    assert((2 | 5) === 7);

    assert((3 | 0) === 3);
    assert((3 | 1) === 3);
    assert((3 | 2) === 3);
    assert((3 | 3) === 3);
    assert((3 | 4) === 7);
    assert((3 | 5) === 7);

    assert((4 | 0) === 4);
    assert((4 | 1) === 5);
    assert((4 | 2) === 6);
    assert((4 | 3) === 7);
    assert((4 | 4) === 4);
    assert((4 | 5) === 5);

    assert((5 | 0) === 5);
    assert((5 | 1) === 5);
    assert((5 | 2) === 7);
    assert((5 | 3) === 7);
    assert((5 | 4) === 5);
    assert((5 | 5) === 5);

    var x = 3;
    var y = 7;
    assert(("42" | 6) === 46);
    assert((x | y) === 7);
    assert((x | [[[[12]]]]) === 15);
    assert((undefined | y) === 7);
    assert(("a" | "b") === 0);
    assert((null | null) === 0);
    assert((undefined | undefined) === 0);
    assert((NaN | NaN) === 0);
    assert((NaN | 6) === 6);
    assert((Infinity | Infinity) === 0);
    assert((-Infinity | Infinity) === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
