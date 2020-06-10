load("test-common.js");

try {
    let o = {
        var1: "foo",
        var2: 42,
        arr: [1, 2, {
            nested: {
                hello: "world",
            },
            get x() { return 10; }
        }],
        obj: {
            subarr: [3],
        },
    };

    let string = JSON.stringify(o, (key, value) => {
        if (key === "hello")
            return undefined;
        if (value === 10)
            return 20;
        if (key === "subarr")
            return [3, 4, 5];
        return value;
    });

    assert(string === '{"var1":"foo","var2":42,"arr":[1,2,{"nested":{},"x":20}],"obj":{"subarr":[3,4,5]}}');

    string = JSON.stringify(o, ["var1", "var1", "var2", "obj"]);
    assert(string == '{"var1":"foo","var2":42,"obj":{}}');

    string = JSON.stringify(o, ["var1", "var1", "var2", "obj", "subarr"]);
    assert(string == '{"var1":"foo","var2":42,"obj":{"subarr":[3]}}');

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
