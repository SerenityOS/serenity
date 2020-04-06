try {
    // FIXME: Remove once we have the global String object
    var String = { prototype: Object.getPrototypeOf("") };

    assert(String.prototype.toLowerCase.length === 0);

    assert("foo".toLowerCase() === "foo");
    assert("Foo".toLowerCase() === "foo");
    assert("FOO".toLowerCase() === "foo");

    assert(('b' + 'a' + + 'a' + 'a').toLowerCase() === "banana");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
