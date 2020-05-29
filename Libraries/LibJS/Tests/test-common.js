/**
 * Custom error for failed assertions.
 * @constructor
 * @param {string} message Error message
 * @returns Error
 */
function AssertionError(message) {
    var instance = new Error(message);
    instance.name = 'AssertionError';
    Object.setPrototypeOf(instance, Object.getPrototypeOf(this));
    return instance;
}

/**
 * Throws an `AssertionError` if `value` is not truthy.
 * @param {*} value Value to be tested
 */
function assert(value) {
    if (!value)
        throw new AssertionError("The assertion failed!");
}

/**
 * Throws an `AssertionError` when called.
 * @throws {AssertionError}
 */
function assertNotReached() {
    throw new AssertionError("assertNotReached() was reached!");
}

/**
 * Ensures the provided functions throws a specific error.
 * @param {Function} testFunction Function executing the throwing code
 * @param {object} [options]
 * @param {Error} [options.error] Expected error type
 * @param {string} [options.name] Expected error name
 * @param {string} [options.message] Expected error message
 */
function assertThrowsError(testFunction, options) {
    try {
        testFunction();
        assertNotReached();
    } catch (e) {
        if (options.error !== undefined)
            assert(e instanceof options.error);
        if (options.name !== undefined)
            assert(e.name === options.name);
        if (options.message !== undefined)
            assert(e.message === options.message);
    }
}

/**
 * Ensures the provided JavaScript source code results in a SyntaxError
 * @param {string} source The JavaScript source code to compile
 */
function assertIsSyntaxError(source) {
    assertThrowsError(() => {
        new Function(source)();
    }, {
        error: SyntaxError,
    });
}

/**
 * Ensures the provided arrays contain exactly the same items.
 * @param {Array} a First array
 * @param {Array} b Second array
 */
function assertArrayEquals(a, b) {
    if (a.length != b.length)
        throw new AssertionError("Array lengths do not match");
    
    for (var i = 0; i < a.length; i++) {
        if (a[i] !== b[i])
            throw new AssertionError("Elements do not match");
    }
}

const assertVisitsAll = (testFunction, expectedOutput) => {
    const visited = [];
    testFunction(value => visited.push(value));
    assert(visited.length === expectedOutput.length);
    expectedOutput.forEach((value, i) => assert(visited[i] === value));
};

/**
 * Check whether the difference between two numbers is less than 0.000001.
 * @param {Number} a First number
 * @param {Number} b Second number
 */
function isClose(a, b) {
    return Math.abs(a - b) < 0.000001;
}
