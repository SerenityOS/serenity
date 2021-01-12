test("basic functionality", () => {
    expect(void "").toBeUndefined();
    expect(void "foo").toBeUndefined();
    expect(void 1).toBeUndefined();
    expect(void 42).toBeUndefined();
    expect(void true).toBeUndefined();
    expect(void false).toBeUndefined();
    expect(void null).toBeUndefined();
    expect(void undefined).toBeUndefined();
    expect(void function () {}).toBeUndefined();
    expect(void (() => {})).toBeUndefined();
    expect(void (() => "hello friends")()).toBeUndefined();
    expect((() => void "hello friends")()).toBeUndefined();
});
