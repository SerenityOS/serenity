load("test-common.js");

try {
    assert(Object.isExtensible() === false);
    assert(Object.isExtensible(undefined) === false);
    assert(Object.isExtensible(null) === false);
    assert(Object.isExtensible(true) === false);
    assert(Object.isExtensible(6) === false);
    assert(Object.isExtensible("test") === false);

    let s = Symbol();
    assert(Object.isExtensible(s) === false);

    let o = { foo: "foo" };
    assert(Object.isExtensible(o) === true);
    Object.preventExtensions(o);
    assert(Object.isExtensible(o) === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
