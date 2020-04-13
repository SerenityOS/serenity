try {
    throw 1;
    assertNotReached();
} catch (e) {
    assert(e === 1);
}

try {
    throw [99];
    assertNotReached();
} catch (e) {
    assert(typeof e === "object");
    assert(e.length === 1);
}

function foo() {
    throw "hello";
    assertNotReached();
}

try {
    foo();
    assertNotReached();
} catch (e) {
    assert(e === "hello");
}

console.log("PASS");
