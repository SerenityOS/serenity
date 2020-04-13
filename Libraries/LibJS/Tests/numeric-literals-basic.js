load("test-common.js");

try {
    assert(0xff === 255);
    assert(0XFF === 255);
    assert(0o10 === 8);
    assert(0O10 === 8);
    assert(0b10 === 2);
    assert(0B10 === 2);
    assert(1e3 === 1000);
    assert(1e+3 === 1000);
    assert(1e-3 === 0.001);
    assert(1. === 1);
    assert(1.e1 === 10);
    assert(.1 === 0.1);
    assert(.1e1 === 1);
    assert(0.1e1 === 1);
    assert(.1e+1 === 1);
    assert(0.1e+1 === 1);

    Number.prototype.foo = 'LOL';
    assert(1..foo === 'LOL');
    assert(1.1.foo === 'LOL');
    assert(.1.foo === 'LOL');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
