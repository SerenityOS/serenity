try {
    var a = [1, 2, 3];
    assert(a.toString() === '1,2,3');
    assert([].toString() === '');
    assert([5].toString() === '5');
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
