load("test-common.js");

Array.prototype.equals = function(other) {
    if (this.length != other.length)
        return false;
    for (var i = 0; i < this.length; i++) {
        if (this[i] != other[i])
            return false;
    }
    return true;
}

try {
    assert(Array.prototype.fill.length === 3);

    var array = [1, 2, 3];
    assert(array.fill(4).equals([4, 4, 4]));
    assert(array.fill(4, 1).equals([1, 4, 4]));
    assert(array.fill(4, 1, 2).equals([1, 4, 3]));
    assert(array.fill(4, 3, 3).equals([1, 2, 3]));
    assert(array.fill(4, -3, -2).equals([4, 2, 3]));
    assert(array.fill(4, NaN, NaN).equals([1, 2, 3]));
    assert(array.fill(4, 3, 5).equals([1, 2, 3]));
    assert(Array(3).fill(4).equals([4, 4, 4]));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
