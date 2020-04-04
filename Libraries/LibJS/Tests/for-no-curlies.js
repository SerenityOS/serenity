function assert(x) { if (!x) throw 1; }

try {
    var number = 0;

    for (var i = 0; i < 3; ++i)
        for (var j = 0; j < 3; ++j)
            number++;

    assert(number === 9);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
