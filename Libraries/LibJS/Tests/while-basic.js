load("test-common.js");

try {
    var number = 0;
    while (number < 9) {
        number++;
    }
    assert(number === 9);

    number = 0;
    while(number < 3) number++;
    assert(number === 3);

    while (false) {
        assertNotReached();
    }

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
