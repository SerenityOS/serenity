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

