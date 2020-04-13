
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
