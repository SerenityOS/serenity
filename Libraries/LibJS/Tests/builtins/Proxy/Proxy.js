test("constructs properly", () => {
    expect(() => {
        new Proxy({}, {});
    }).not.toThrow();
});

test("constructor argument count", () => {
    expect(() => {
        new Proxy();
    }).toThrowWithMessage(TypeError, "Proxy constructor requires at least two arguments");

    expect(() => {
        new Proxy({});
    }).toThrowWithMessage(TypeError, "Proxy constructor requires at least two arguments");
});

test("constructor requires objects", () => {
    expect(() => {
        new Proxy(1, {});
    }).toThrowWithMessage(
        TypeError,
        "Expected target argument of Proxy constructor to be object, got 1"
    );

    expect(() => {
        new Proxy({}, 1);
    }).toThrowWithMessage(
        TypeError,
        "Expected handler argument of Proxy constructor to be object, got 1"
    );
});

test("constructor must be invoked with 'new'", () => {
    expect(() => {
        Proxy({}, {});
    }).toThrowWithMessage(TypeError, "Proxy must be called with the 'new' operator");
});
