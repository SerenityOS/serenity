test("basic functionality", () => {
    const o = {};
    o.a = 1;

    expect(o.a === 1).toBeTrue();
    expect(!o.a === false).toBeTrue();
    expect(!o.a === !o.a).toBeTrue();
    expect(~o.a === ~o.a).toBeTrue();
    expect(+o.a === +o.a).toBeTrue();
    expect(-o.a === -o.a).toBeTrue();

    expect((typeof "x" === "string") === true).toBeTrue();
    expect(!(typeof "x" === "string") === false).toBeTrue();
});
