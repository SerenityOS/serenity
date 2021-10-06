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

test("Issue #8198 arrow function escapes function scope", () => {
    const b = 3;

    function f() {
        expect(b).toBe(3);
        (() => {
            expect(b).toBe(3);
            var a = "wat";
            eval("var b=a;");
            expect(b).toBe("wat");
        })();
        expect(b).toBe(3);
    }

    f();
    expect(b).toBe(3);
});
