
function AssertionError(message) {
    var instance = new Error(message);
    instance.name = 'AssertionError';
    Object.setPrototypeOf(instance, Object.getPrototypeOf(this));
    return instance;
}

function assert(value) {
    if (!value)
        throw new AssertionError("The assertion failed!");
}

function assertNotReached() {
    throw new AssertionError("assertNotReached() was reached!");
}

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

