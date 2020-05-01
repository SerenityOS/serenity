load("test-common.js");

try {
    const o = {
        foo: "bar",
        getFoo() {
            return this.foo;
        },
        12() {
            return this.getFoo();
        },
        "hello friends"() {
            return this.getFoo();
        },
        [4 + 10]() {
            return this.getFoo();
        },
    };

    assert(o.foo === "bar");
    assert(o.getFoo() === "bar");
    assert(o[12]() === "bar");
    assert(o["hello friends"]() === "bar");
    assert(o[14]() === "bar");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
