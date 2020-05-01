load("test-common.js");

try {
    assert(Reflect.ownKeys.length === 1);

    [null, undefined, "foo", 123, NaN, Infinity].forEach(value => {
        assertThrowsError(() => {
            Reflect.ownKeys(value);
        }, {
            error: TypeError,
            message: "First argument of Reflect.ownKeys() must be an object"
        });
    });

    var objectOwnKeys = Reflect.ownKeys({});
    assert(objectOwnKeys.length === 0);

    objectOwnKeys = Reflect.ownKeys({ foo: "bar", bar: "baz", 0: 42 });
    assert(objectOwnKeys.length === 3);
    assert(objectOwnKeys[0] === "0");
    assert(objectOwnKeys[1] === "foo");
    assert(objectOwnKeys[2] === "bar");

    var arrayOwnKeys = Reflect.ownKeys([]);
    assert(arrayOwnKeys.length === 1);
    assert(arrayOwnKeys[0] === "length");

    arrayOwnKeys = Reflect.ownKeys(["foo", [], 123, undefined]);
    assert(arrayOwnKeys.length === 5);
    assert(arrayOwnKeys[0] === "0");
    assert(arrayOwnKeys[1] === "1");
    assert(arrayOwnKeys[2] === "2");
    assert(arrayOwnKeys[3] === "3");
    assert(arrayOwnKeys[4] === "length");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
