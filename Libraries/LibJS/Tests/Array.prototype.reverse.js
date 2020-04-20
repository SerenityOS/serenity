load("test-common.js");

try {
    assert(Array.prototype.reverse.length === 0);

    var array = [1, 2, 3];
    
    assert(array[0] === 1);
    assert(array[1] === 2);
    assert(array[2] === 3);
    
    array.reverse();
    
    assert(array[0] === 3);
    assert(array[1] === 2);
    assert(array[2] === 1);
    
    var array_ref = array.reverse();
    
    assert(array_ref[0] === 1);
    assert(array_ref[1] === 2);
    assert(array_ref[2] === 3);

    assert(array[0] === 1);
    assert(array[1] === 2);
    assert(array[2] === 3);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
