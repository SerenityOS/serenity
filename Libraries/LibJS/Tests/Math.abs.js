function assert(x) { if (!x) throw 1; }

try {
    assert(Math.abs('-1') === 1);
    assert(Math.abs(0 - 2) === 2);
    assert(Math.abs(null) === 0);
    assert(Math.abs('') === 0);
    assert(Math.abs([]) === 0);
    assert(Math.abs([2]) === 2);
    assert(isNaN(Math.abs([1, 2])));
    assert(isNaN(Math.abs({})));
    assert(isNaN(Math.abs('string')));
    assert(isNaN(Math.abs()));
    console.log("PASS");
} catch {
}
