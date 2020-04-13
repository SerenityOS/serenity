load("test-common.js");

try {
    var o = {};
    o.a = 1;

    assert(o.a === 1);
    assert(!o.a === false);
    assert(!o.a === !(o.a));
    assert(~o.a === ~(o.a));
    assert(+o.a === +(o.a));
    assert(-o.a === -(o.a));

    assert((typeof "x" === "string") === true);
    assert(!(typeof "x" === "string") === false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
