load("test-common.js");

try {
    assert(void "" === undefined);
    assert(void "foo" === undefined);
    assert(void 1 === undefined);
    assert(void 42 === undefined);
    assert(void true === undefined);
    assert(void false === undefined);
    assert(void null === undefined);
    assert(void undefined === undefined);
    assert(void function () {} === undefined);
    assert(void (() => {}) === undefined);
    assert(void (() => "hello friends")() === undefined);
    assert((() => void "hello friends")() === undefined);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
