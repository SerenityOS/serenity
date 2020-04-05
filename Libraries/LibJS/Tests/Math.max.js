try {
    assert(Math.max.length === 2);
    assert(Math.max(1) === 1);
    assert(Math.max(2, 1) === 2);
    assert(Math.max(1, 2, 3) === 3);
    assert(isNaN(Math.max(NaN)));
    assert(isNaN(Math.max("String", 1)));

    console.log("PASS");
} catch {
    console.log("FAIL");
}
