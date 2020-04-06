try {
    // FIXME: Remove once we have the global String object
    var String = { prototype: Object.getPrototypeOf("") };

    assert(String.prototype.toUpperCase.length === 0);

    assert("foo".toUpperCase() === "FOO");
    assert("Foo".toUpperCase() === "FOO");
    assert("FOO".toUpperCase() === "FOO");

    assert(('b' + 'a' + + 'n' + 'a').toUpperCase() === "BANANA");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
