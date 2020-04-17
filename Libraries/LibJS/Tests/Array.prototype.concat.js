load("test-common.js");

try {
    assert(Array.prototype.concat.length === 1);

    var array = ["hello", "friends"];

    var array_concat = array.concat();
    assert(array_concat.length === array.length);

    array_concat = array.concat(1)
    assert(array_concat.length === 3);
    assert(array_concat[2] === 1);

    array_concat = array.concat([1, 2, 3])
    assert(array_concat.length === 5);
    assert(array_concat[2] === 1);
    assert(array_concat[3] === 2);
    assert(array_concat[4] === 3);

    array_concat = array.concat(false, "serenity");
    assert(array_concat.length === 4);
    assert(array_concat[2] === false);
    assert(array_concat[3] === "serenity");

    array_concat = array.concat({ name: "libjs" }, [1, [2, 3]]);
    assert(array_concat.length === 5);
    assert(array_concat[2].name === "libjs");
    assert(array_concat[3] === 1);
    assert(array_concat[4][0] === 2);
    assert(array_concat[4][1] === 3);


    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
