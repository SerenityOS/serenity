a = 1;

test("basic functionality", () => {
    expect(delete a).toBeTrue();

    expect(() => {
        a;
    }).toThrowWithMessage(ReferenceError, "'a' is not defined");
});
