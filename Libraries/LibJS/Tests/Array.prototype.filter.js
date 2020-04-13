load("test-common.js");

try {
    assert(Array.prototype.filter.length === 1);

    try {
        [].filter();
        assertNotReached();
    } catch (e) {
        assert(e.name === "TypeError");
        assert(e.message === "Array.prototype.filter() requires at least one argument");
    }

    try {
        [].filter(undefined);
        assertNotReached();
    } catch (e) {
        assert(e.name === "TypeError");
        assert(e.message === "undefined is not a function");
    }

    var callbackCalled = 0;
    var callback = () => { callbackCalled++; };

    assert([].filter(callback).length === 0);
    assert(callbackCalled === 0);

    assert([1, 2, 3].filter(callback).length === 0);
    assert(callbackCalled === 3);

    var evenNumbers = [0, 1, 2, 3, 4, 5, 6, 7].filter(x => x % 2 === 0);
    assert(evenNumbers.length === 4);
    assert(evenNumbers[0] === 0);
    assert(evenNumbers[1] === 2);
    assert(evenNumbers[2] === 4);
    assert(evenNumbers[3] === 6);

    var fruits = ["Apple", "Banana", "Blueberry", "Grape", "Mango", "Orange", "Peach", "Pineapple", "Raspberry", "Watermelon"];
    const filterItems = (arr, query) => {
        return arr.filter(el => el.toLowerCase().indexOf(query.toLowerCase()) !== -1)
    };

    var results;

    results = filterItems(fruits, "Berry");
    assert(results.length === 2);
    assert(results[0] === "Blueberry");
    assert(results[1] === "Raspberry");

    results = filterItems(fruits, "P");
    assert(results.length === 5);
    assert(results[0] === "Apple");
    assert(results[1] === "Grape");
    assert(results[2] === "Peach");
    assert(results[3] === "Pineapple");
    assert(results[4] === "Raspberry");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
