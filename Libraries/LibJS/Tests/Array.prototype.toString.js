try {
    var a = [1, 2, 3];
    assert(a.toString() === '1,2,3');
    assert([].toString() === '');
    assert([5].toString() === '5');

    assert("rgb(" + [10, 11, 12] + ")" === "rgb(10,11,12)");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
