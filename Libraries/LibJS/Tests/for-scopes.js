load("test-common.js");

try {
    for (var v = 5; false; );
    assert(v == 5);

    const options = {error: ReferenceError};

    assertThrowsError(() => {
        for (let l = 5; false; );
        l;
    }, options);

    assertThrowsError(() => {
        for (const c = 5; false; );
        c;
    }, options)

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
