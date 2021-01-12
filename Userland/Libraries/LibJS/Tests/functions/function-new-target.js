test("basic functionality", () => {
    function foo() {
        return new.target;
    }
    expect(foo()).toBeUndefined();
    expect(new foo()).toEqual(foo);

    function bar() {
        const baz = () => new.target;
        return baz();
    }
    expect(bar()).toBeUndefined();
    expect(new bar()).toEqual(bar);

    class baz {
        constructor() {
            this.newTarget = new.target;
        }
    }
    expect(new baz().newTarget).toEqual(baz);
});

test("syntax error outside of function", () => {
    expect("new.target").not.toEval();
});
