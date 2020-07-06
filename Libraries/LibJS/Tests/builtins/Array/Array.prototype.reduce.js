load("test-common.js");

try {
    assert(Array.prototype.reduce.length === 1);

    assertThrowsError(
        () => {
            [1].reduce();
        },
        {
            error: TypeError,
            message: "Array.prototype.reduce() requires at least one argument",
        }
    );

    assertThrowsError(
        () => {
            [1].reduce(undefined);
        },
        {
            error: TypeError,
            message: "undefined is not a function",
        }
    );

    assertThrowsError(
        () => {
            [].reduce((a, x) => x);
        },
        {
            error: TypeError,
            message: "Reduce of empty array with no initial value",
        }
    );

    assertThrowsError(
        () => {
            [, ,].reduce((a, x) => x);
        },
        {
            error: TypeError,
            message: "Reduce of empty array with no initial value",
        }
    );

    [1, 2].reduce(function () {
        assert(this === undefined);
    });

    var callbackCalled = 0;
    var callback = () => {
        callbackCalled++;
        return true;
    };

    assert([1].reduce(callback) === 1);
    assert(callbackCalled === 0);

    assert([, 1].reduce(callback) === 1);
    assert(callbackCalled === 0);

    callbackCalled = 0;
    assert([1, 2, 3].reduce(callback) === true);
    assert(callbackCalled === 2);

    callbackCalled = 0;
    assert([, , 1, 2, 3].reduce(callback) === true);
    assert(callbackCalled === 2);

    callbackCalled = 0;
    assert([1, , , 10, , 100, , ,].reduce(callback) === true);
    assert(callbackCalled === 2);

    var constantlySad = () => ":^(";
    var result = [].reduce(constantlySad, ":^)");
    assert(result === ":^)");

    result = [":^0"].reduce(constantlySad, ":^)");
    assert(result === ":^(");

    result = [":^0"].reduce(constantlySad);
    assert(result === ":^0");

    result = [5, 4, 3, 2, 1].reduce((accum, elem) => accum + elem);
    assert(result === 15);

    result = [1, 2, 3, 4, 5, 6].reduce((accum, elem) => accum + elem, 100);
    assert(result === 121);

    result = [6, 5, 4, 3, 2, 1].reduce((accum, elem) => {
        return accum + elem;
    }, 100);
    assert(result === 121);

    var indexes = [];
    result = ["foo", 1, true].reduce((a, v, i) => {
        indexes.push(i);
    });
    assert(result === undefined);
    assert(indexes.length === 2);
    assert(indexes[0] === 1);
    assert(indexes[1] === 2);

    indexes = [];
    result = ["foo", 1, true].reduce((a, v, i) => {
        indexes.push(i);
    }, "foo");
    assert(result === undefined);
    assert(indexes.length === 3);
    assert(indexes[0] === 0);
    assert(indexes[1] === 1);
    assert(indexes[2] === 2);

    var mutable = { prop: 0 };
    result = ["foo", 1, true].reduce((a, v) => {
        a.prop = v;
        return a;
    }, mutable);
    assert(result === mutable);
    assert(result.prop === true);

    var a1 = [1, 2];
    var a2 = null;
    a1.reduce((a, v, i, t) => {
        a2 = t;
    });
    assert(a1 === a2);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
