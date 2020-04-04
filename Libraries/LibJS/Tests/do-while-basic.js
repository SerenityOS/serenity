function assert(x) { if (!x) throw 1; }

try {
    var number = 0;
    do {
        number++;
    } while (number < 9);
    assert(number === 9);

    number = 0;
    do number++; while(number < 3);
    assert(number === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
