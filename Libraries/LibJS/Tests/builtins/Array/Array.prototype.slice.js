load("test-common.js");

try {
    assert(Array.prototype.slice.length === 2);

    var array = ["hello", "friends", "serenity", 1];

    var array_slice = array.slice();
    assert(array_slice.length === array.length);
    assert(array_slice.length === 4);
    assert(array_slice[0] === "hello");
    assert(array_slice[1] === "friends");
    assert(array_slice[2] === "serenity");
    assert(array_slice[3] === 1);

    array_slice = array.slice(1)
    assert(array_slice.length === 3);
    assert(array_slice[0] === "friends");
    assert(array_slice[1] === "serenity");
    assert(array_slice[2] === 1);

    array_slice = array.slice(0, 2);
    assert(array_slice.length === 2);
    assert(array_slice[0] === "hello");
    assert(array_slice[1] === "friends");

    array_slice = array.slice(-1);
    assert(array_slice.length === 1);
    assert(array_slice[0] === 1);

    array_slice = array.slice(1, 1);
    assert(array_slice.length === 0);
    
    array_slice = array.slice(1, -1);
    assert(array_slice.length === 2);
    assert(array_slice[0] === "friends");
    assert(array_slice[1] === "serenity");
    
    array_slice = array.slice(2, -1);
    assert(array_slice.length === 1);
    assert(array_slice[0] === "serenity");

    array_slice = array.slice(0, 100);
    assert(array_slice.length === 4);
    assert(array_slice[0] === "hello");
    assert(array_slice[1] === "friends");
    assert(array_slice[2] === "serenity");
    assert(array_slice[3] === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
