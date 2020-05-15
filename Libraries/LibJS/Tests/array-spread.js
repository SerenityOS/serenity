load("test-common.js");

function testArray(arr) {
    return arr.length === 4 &&
        arr[0] === 0 &&
        arr[1] === 1 &&
        arr[2] === 2 &&
        arr[3] === 3;
}

try {
    let arr = [0, ...[1, 2], 3];
    assert(testArray(arr));

    let a = [1, 2];
    arr = [0, ...a, 3];
    assert(testArray(arr));

    let obj = { a: [1, 2] };
    arr = [0, ...obj.a, 3];
    assert(testArray(arr));

    arr = [...[], ...[...[0, 1, 2]], 3];
    assert(testArray(arr));

    assertThrowsError(() => {
        [...1];
    }, {
        error: TypeError,
        message: "1 is not iterable",
    });


    assertThrowsError(() => {
        [...{}];
    }, {
        error: TypeError,
        message: "[object Object] is not iterable",
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
