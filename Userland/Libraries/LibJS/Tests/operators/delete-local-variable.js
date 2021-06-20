test("basic functionality", () => {
    let a = 5;
    expect(delete a).toBeTrue();

    expect(() => {
        a;
    }).toThrowWithMessage(ReferenceError, "'a' is not defined");
});
