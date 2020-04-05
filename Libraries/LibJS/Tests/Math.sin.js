try {
    assert(Math.sin(0) === 0);
    assert(Math.sin(null) === 0);
    assert(Math.sin('') === 0);
    assert(Math.sin([]) === 0);
    assert(Math.sin(Math.PI * 3 / 2) === -1);
    assert(Math.sin(Math.PI / 2) === 1);
    assert(isNaN(Math.sin()));
    assert(isNaN(Math.sin(undefined)));
    assert(isNaN(Math.sin([1, 2, 3])));
    assert(isNaN(Math.sin({})));
    assert(isNaN(Math.sin("foo")));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
