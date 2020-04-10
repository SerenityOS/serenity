try {
    assert(String.prototype.toUpperCase.length === 0);

    assert("foo".toUpperCase() === "FOO");
    assert("Foo".toUpperCase() === "FOO");
    assert("FOO".toUpperCase() === "FOO");

    assert(('b' + 'a' + + 'n' + 'a').toUpperCase() === "BANANA");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
