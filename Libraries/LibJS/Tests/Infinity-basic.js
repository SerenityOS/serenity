function assert(x) { if (!x) throw 1; }

try {
    assert(Infinity + "" === "Infinity");
    assert(-Infinity + "" === "-Infinity");
    assert(Infinity === Infinity);
    assert(Infinity - 1 === Infinity);
    assert(Infinity + 1 === Infinity);
    assert(-Infinity === -Infinity);
    assert(-Infinity - 1 === -Infinity);
    assert(-Infinity + 1 === -Infinity);
    assert(1 / Infinity === 0);
    assert(1 / -Infinity === 0);
    assert(1 / 0 === Infinity);
    assert(-1 / 0 === -Infinity);
    assert(-100 < Infinity);
    assert(0 < Infinity);
    assert(100 < Infinity);
    assert(-Infinity < Infinity);
    assert(-100 > -Infinity);
    assert(0 > -Infinity);
    assert(100 > -Infinity);
    assert(Infinity > -Infinity);
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
