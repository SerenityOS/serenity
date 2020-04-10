try {
    assert(String.prototype.toLowerCase.length === 0);

    assert("foo".toLowerCase() === "foo");
    assert("Foo".toLowerCase() === "foo");
    assert("FOO".toLowerCase() === "foo");

    assert(('b' + 'a' + + 'a' + 'a').toLowerCase() === "banana");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
