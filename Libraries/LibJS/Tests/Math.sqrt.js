function assert(x) { if (!x) throw 1; }

try {
    assert(Math.sqrt(9) === 3);
    console.log("PASS");
} catch {
}
