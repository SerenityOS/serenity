test("basic functionality", () => {
    function Foo() {}
    Foo[Symbol.hasInstance] = value => {
        return value === 2;
    };

    expect(new Foo() instanceof Foo).toBeFalse();
    expect(2 instanceof Foo).toBeTrue();
});
