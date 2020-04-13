function assert(x) { if(!x) throw x; }

try {
    assert(String.prototype.trim.length === 0)
    assert("   hello friends  ".trim() === "hello friends");
    assert("hello friends   ".trim() === "hello friends");
    assert("   hello friends".trim() === "hello friends");
    assert("   hello friends".trimStart() === "hello friends");
    assert("hello friends   ".trimEnd() === "hello friends");
    

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
