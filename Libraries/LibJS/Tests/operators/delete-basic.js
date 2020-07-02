load("test-common.js");

try {
    o = {};
    o.x = 1;
    o.y = 2;
    o.z = 3;
    assert(Object.getOwnPropertyNames(o).length === 3);

    assert(delete o.x === true);
    assert(o.hasOwnProperty('x') === false);
    assert(o.hasOwnProperty('y') === true);
    assert(o.hasOwnProperty('z') === true);
    assert(Object.getOwnPropertyNames(o).length === 2);

    assert(delete o.y === true);
    assert(o.hasOwnProperty('x') === false);
    assert(o.hasOwnProperty('y') === false);
    assert(o.hasOwnProperty('z') === true);
    assert(Object.getOwnPropertyNames(o).length === 1);

    assert(delete o.z === true);
    assert(o.hasOwnProperty('x') === false);
    assert(o.hasOwnProperty('y') === false);
    assert(o.hasOwnProperty('z') === false);
    assert(Object.getOwnPropertyNames(o).length === 0);

    a = [ 3, 5, 7 ];

    assert(Object.getOwnPropertyNames(a).length === 4);

    assert(delete a[0] === true);
    assert(a.hasOwnProperty(0) === false);
    assert(a.hasOwnProperty(1) === true);
    assert(a.hasOwnProperty(2) === true);
    assert(Object.getOwnPropertyNames(a).length === 3);

    assert(delete a[1] === true);
    assert(a.hasOwnProperty(0) === false);
    assert(a.hasOwnProperty(1) === false);
    assert(a.hasOwnProperty(2) === true);
    assert(Object.getOwnPropertyNames(a).length === 2);

    assert(delete a[2] === true);
    assert(a.hasOwnProperty(0) === false);
    assert(a.hasOwnProperty(1) === false);
    assert(a.hasOwnProperty(2) === false);
    assert(Object.getOwnPropertyNames(a).length === 1);

    q = {};
    Object.defineProperty(q, "foo", { value: 1, writable: false, enumerable: false });
    assert(q.foo === 1);

    assert(delete q.foo === false);
    assert(q.hasOwnProperty('foo') === true);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
