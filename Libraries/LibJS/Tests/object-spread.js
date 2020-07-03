const testObjSpread = obj => {
    expect(obj).toEqual({
        foo: 0,
        bar: 1,
        baz: 2,
        qux: 3
    });
};

const testObjStrSpread = obj => {
    expect(obj).toEqual(["a", "b", "c", "d"]);
};

test("spread object literal inside object literal", () => {
    let obj = { 
        foo: 0, 
        ...{ bar: 1, baz: 2 }, 
        qux: 3,
    };
    testObjSpread(obj);
});

test("spread object with assigned property inside object literal", () => {
    obj = { foo: 0, bar: 1, baz: 2 };
    obj.qux = 3;
    testObjSpread({ ...obj });
});

test("spread object inside object literal", () => {
    let a = { bar: 1, baz: 2 };
    obj = { foo: 0, ...a, qux: 3 };
    testObjSpread(obj);
});

test("complex nested object spreading", () => {
    obj = {
        ...{},
        ...{
            ...{ foo: 0, bar: 1, baz: 2 },
        },
        qux: 3,
    };
    testObjSpread(obj);
});

test("spread string in object literal", () => {
    obj = { ..."abcd" };
    testObjStrSpread(obj);
});

test("spread array in object literal", () => {
    obj = { ...["a", "b", "c", "d"] };
    testObjStrSpread(obj);
});

test("spread string object in object literal", () => {
    obj = { ...String("abcd") };
    testObjStrSpread(obj);
});

test("spread object with non-enumerable property", () => {
    a = { foo: 0 };
    Object.defineProperty(a, "bar", {
        value: 1,
        enumerable: false,
    });
    obj = { ...a };
    expect(obj.foo).toBe(0);
    expect(obj).not.toHaveProperty("bar");
});

test("spreading non-spreadable values", () => {
    let empty = ({
        ...undefined,
        ...null,
        ...1,
        ...true,
        ...function(){},
        ...Date,
    });
    expect(Object.getOwnPropertyNames(empty)).toHaveLength(0);
});
