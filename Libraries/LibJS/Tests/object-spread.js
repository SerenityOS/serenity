load("test-common.js");

function testObjSpread(obj) {
    return obj.foo === 0 && 
        obj.bar === 1 &&
        obj.baz === 2 &&
        obj.qux === 3;
}

function testObjStrSpread(obj) {
    return obj[0] === "a" && 
        obj[1] === "b" && 
        obj[2] === "c" &&
        obj[3] === "d";
}

try {
    let obj = { 
        foo: 0, 
        ...{ bar: 1, baz: 2 }, 
        qux: 3,
    };
    assert(testObjSpread(obj));

    obj = { foo: 0, bar: 1, baz: 2 };
    obj.qux = 3;
    assert(testObjSpread({ ...obj }));

    let a = { bar: 1, baz: 2 };
    obj = { foo: 0, ...a, qux: 3 };
    assert(testObjSpread(obj));

    obj = {
        ...{},
        ...{
            ...{ foo: 0, bar: 1, baz: 2 },
        },
        qux: 3,
    };
    assert(testObjSpread(obj));

    obj = { ..."abcd" };
    assert(testObjStrSpread(obj));
    
    obj = { ...["a", "b", "c", "d"] };
    assert(testObjStrSpread(obj));
    
    obj = { ...String("abcd") };
    assert(testObjStrSpread(obj));

    a = { foo: 0 };
    Object.defineProperty(a, 'bar', {
        value: 1,
        enumerable: false,
    });
    obj = { ...a };
    assert(obj.foo === 0 && obj.bar === undefined);

    let empty = ({
        ...undefined,
        ...null,
        ...1,
        ...true,
        ...function(){},
        ...Date,
    });
    assert(Object.getOwnPropertyNames(empty).length === 0);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}