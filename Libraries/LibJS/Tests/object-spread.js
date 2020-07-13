const testObjSpread = obj => {
    expect(obj).toEqual({
        foo: 0,
        bar: 1,
        baz: 2,
        qux: 3,
    });
};

const testObjStrSpread = obj => {
    expect(obj).toEqual(["a", "b", "c", "d"]);
};

test("spread object literal inside object literal", () => {
    const obj = {
        foo: 0,
        ...{ bar: 1, baz: 2 },
        qux: 3,
    };
    testObjSpread(obj);
});

test("spread object with assigned property inside object literal", () => {
    const obj = { foo: 0, bar: 1, baz: 2 };
    obj.qux = 3;
    testObjSpread({ ...obj });
});

test("spread object inside object literal", () => {
    let a = { bar: 1, baz: 2 };
    const obj = { foo: 0, ...a, qux: 3 };
    testObjSpread(obj);
});

test("complex nested object spreading", () => {
    const obj = {
        ...{},
        ...{
            ...{ foo: 0, bar: 1, baz: 2 },
        },
        qux: 3,
    };
    testObjSpread(obj);
});

test("spread string in object literal", () => {
    const obj = { ..."abcd" };
    testObjStrSpread(obj);
});

test("spread array in object literal", () => {
    const obj = { ...["a", "b", "c", "d"] };
    testObjStrSpread(obj);
});

test("spread string object in object literal", () => {
    const obj = { ...String("abcd") };
    testObjStrSpread(obj);
});

test("spread object with non-enumerable property", () => {
    const a = { foo: 0 };
    Object.defineProperty(a, "bar", {
        value: 1,
        enumerable: false,
    });
    const obj = { ...a };
    expect(obj.foo).toBe(0);
    expect(obj).not.toHaveProperty("bar");
});

test("spread object with symbol keys", () => {
    const s = Symbol("baz");
    const a = {
        foo: "bar",
        [s]: "qux",
    };
    const obj = { ...a };
    expect(obj.foo).toBe("bar");
    expect(obj[s]).toBe("qux");
});

test("spreading non-spreadable values", () => {
    let empty = {
        ...undefined,
        ...null,
        ...1,
        ...true,
        ...function () {},
        ...Date,
    };
    expect(Object.getOwnPropertyNames(empty)).toHaveLength(0);
});

test("respects custom Symbol.iterator method", () => {
    let o = {
        [Symbol.iterator]() {
            return {
                i: 0,
                next() {
                    if (this.i++ == 3) {
                        return { done: true };
                    }
                    return { value: this.i, done: false };
                },
            };
        },
    };

    let a = [...o];
    expect(a).toEqual([1, 2, 3]);
});
