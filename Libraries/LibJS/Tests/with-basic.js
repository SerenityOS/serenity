test("basic with statement functionality", () => {
    var object = { "foo": 5, "bar": 6, "baz": 7 };
    var qux = 1;

    var bar = 99;

    with (object) {
        expect(foo).toBe(5);
        expect(bar).toBe(6);
        expect(baz).toBe(7);
        expect(qux).toBe(1);
        expect(typeof quz).toBe("undefined");

        bar = 2;
    }

    expect(object.bar).toBe(2);

    expect(bar).toBe(99);
});
