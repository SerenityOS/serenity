load("test-common.js")

try {
    var i = 0;

    // i++;
    /* i++; */
    /*
    i++;
    */
    <!-- i++; --> i++;
    <!-- i++;
    i++;
    --> i++;

    assert(i === 1);

    console.log('PASS');
} catch (e) {
    console.log('FAIL: ' + e);
}

