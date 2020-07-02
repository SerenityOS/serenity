load("test-common.js");

try {
    assert(String.prototype.includes.length === 1);

    assert("hello friends".includes("hello") === true);
    assert("hello friends".includes("hello", 100) === false);
    assert("hello friends".includes("hello", -10) === true);
    assert("hello friends".includes("friends", 6) === true);
    assert("hello friends".includes("hello", 6) === false);
    assert("hello friends false".includes(false) === true);
    assert("hello 10 friends".includes(10) === true);
    assert("hello friends undefined".includes() === true);

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
