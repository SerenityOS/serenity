try {
    assert(typeof "foo" === "string");
    assert(!(typeof "foo" !== "string"));
    assert(typeof (1 + 2) === "number");
    assert(typeof {} === "object");
    assert(typeof null === "object");
    assert(typeof undefined === "undefined");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
