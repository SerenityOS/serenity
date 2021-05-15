test("basic functionality", () => {
    function foo() {
        i = 3;
        expect(i).toBe(3);
        var i;
    }

    foo();

    var caught_exception;
    try {
        j = i;
    } catch (e) {
        caught_exception = e;
    }
    expect(caught_exception).not.toBeUndefined();
});
