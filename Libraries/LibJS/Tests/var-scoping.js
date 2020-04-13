load("test-common.js");

try {
    function foo() {
        i = 3;
        assert(i === 3);
        var i;
    }

    foo();

    var caught_exception;
    try {
        j = i;
    } catch (e) {
        caught_exception = e;
    }
    assert(caught_exception !== undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
