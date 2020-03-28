function assert(x) { if (!x) console.log("FAIL"); }

try {
    i < 3;
} catch (e) {
    assert(e.name === "ReferenceError");
}

console.log("PASS");
