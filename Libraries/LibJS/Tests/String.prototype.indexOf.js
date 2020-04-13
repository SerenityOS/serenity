load("test-common.js");

try {
    var s = "hello friends"

    assert(s.indexOf("friends") === 6);
    assert(s.indexOf("enemies") === -1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
