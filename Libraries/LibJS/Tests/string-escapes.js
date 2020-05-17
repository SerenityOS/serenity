load("test-common.js")

try {
    assert("\x55" === "U");
    assert("\X55" === "X55");
    assert(`\x55` === "U");
    assert(`\X55` === "X55");

    assert("\u26a0" === "⚠");
    assert(`\u26a0` === "⚠");
    assert("\u{1f41e}" === "🐞");
    assert(`\u{1f41e}` === "🐞");
    
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
