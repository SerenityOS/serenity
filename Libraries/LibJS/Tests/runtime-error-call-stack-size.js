test("infinite recursion", () => {
    function infiniteRecursion() {
        infiniteRecursion();
    }

    try {
        infiniteRecursion();
    } catch (e) {
        expect(e).toBeInstanceOf(Error);
        expect(e.name).toBe("RuntimeError");
        expect(e.message).toBe("Call stack size limit exceeded");
    }

    expect(() => {
        JSON.stringify({}, () => ({ foo: "bar" }));
    }).toThrowWithMessage(Error, "Call stack size limit exceeded");

    expect(() => {
        new Proxy({}, { get: (_, __, p) => p.foo }).foo;
    }).toThrowWithMessage(Error, "Call stack size limit exceeded");
});
