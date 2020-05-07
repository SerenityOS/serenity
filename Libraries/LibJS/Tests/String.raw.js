load("test-common.js")

try {
    let str = String.raw`foo\nbar`;
    assert(str.length === 8 && str === "foo\\nbar");

    str = String.raw`foo ${1 + 9}\nbar${"hf!"}`;
    assert(str === "foo 10\\nbarhf!");

    str = String.raw`${10}${20}${30}`;
    assert(str === "102030");

    str = String.raw({ raw: ["foo ", "\\nbar"] }, 10, "hf!");
    assert(str === "foo 10\\nbar");

    str = String.raw({ raw: ["foo ", "\\nbar"] });
    assert(str === "foo \\nbar");

    str = String.raw({ raw: [] }, 10, "hf!");
    assert(str === "");

    str = String.raw({ raw: 1 });
    assert(str === "");

    assertThrowsError(() => {
        String.raw({});
    }, {
        error: TypeError,
        message: "Cannot convert property 'raw' to object from undefined",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
