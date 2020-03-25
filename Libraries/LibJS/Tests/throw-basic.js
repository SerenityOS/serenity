function assert(x) { if (!x) console.log("FAIL"); }

try {
    throw 1;
} catch (e) {
    assert(e === 1);
}

try {
    throw [99];
} catch (e) {
    assert(typeof e === "object");
    assert(e.length === 1);
}

function foo() {
    throw "hello";
}

try {
    foo();
} catch (e) {
    assert(e === "hello");
}

console.log("PASS");
