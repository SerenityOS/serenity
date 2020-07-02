load("test-common.js");

try {
    let string = `{"var1":10,"var2":"hello","var3":{"nested":5}}`;

    let object = JSON.parse(string, (key, value) => typeof value === "number" ? value * 2 : value);
    assertDeepEquals(object, { var1: 20, var2: "hello", var3: { nested: 10 } });

    object = JSON.parse(string, (key, value) => typeof value === "number" ? undefined : value);
    assertDeepEquals(object, { var2: "hello", var3: {} });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
