try {
    assert(Array.prototype.constructor === Array)
    assert(Boolean.prototype.constructor === Boolean)
    assert(Date.prototype.constructor === Date)
    assert(Error.prototype.constructor === Error)
    assert(Function.prototype.constructor === Function)
    assert(Number.prototype.constructor === Number)
    assert(Object.prototype.constructor === Object)

    o = {}
    assert(o.constructor === Object)

    o = new Object
    assert(o.constructor === Object)

    a = []
    assert(a.constructor === Array)

    a = new Array
    assert(a.constructor === Array)

    n = new Number(3)
    assert(n.constructor === Number)

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
