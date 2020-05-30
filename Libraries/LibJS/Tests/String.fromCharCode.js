load("test-common.js");

try {
    assert(String.fromCharCode.length === 1);
    
    assert(String.fromCharCode() === "");
    assert(String.fromCharCode(0) === "\u0000");
    assert(String.fromCharCode(false) === "\u0000");
    assert(String.fromCharCode(null) === "\u0000");
    assert(String.fromCharCode(undefined) === "\u0000");
    assert(String.fromCharCode(1) === "\u0001");
    assert(String.fromCharCode(true) === "\u0001");
    assert(String.fromCharCode(-1) === "\uffff");
    assert(String.fromCharCode(0xffff) === "\uffff");
    assert(String.fromCharCode(0x123ffff) === "\uffff");
    assert(String.fromCharCode(65) === "A");
    assert(String.fromCharCode(65, 66, 67) === "ABC");
    assert(String.fromCharCode(228, 246, 252) === "äöü");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
