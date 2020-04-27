load("test-common.js");

function testArray(arr) {
    return arr.length === 4 &&
        arr[0] === "a" &&
        arr[1] === "b" &&
        arr[2] === "c" &&
        arr[3] === "d";
}

try {
    var arr;

    arr = ["a", ..."bc", "d"];
    assert(testArray(arr));

    let s = "bc";
    arr = ["a", ...s, "d"];
    assert(testArray(arr));

    let obj = { a: "bc" };
    arr = ["a", ...obj.a, "d"];
    assert(testArray(arr));

    arr = [..."", ...[...new String("abc")], "d"];
    assert(testArray(arr));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
