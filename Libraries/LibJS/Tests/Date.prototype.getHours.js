try {
    var d = new Date();
    assert(!isNaN(d.getHours()));
    assert(0 <= d.getHours() <= 23);
    assert(d.getHours() === d.getHours());
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
