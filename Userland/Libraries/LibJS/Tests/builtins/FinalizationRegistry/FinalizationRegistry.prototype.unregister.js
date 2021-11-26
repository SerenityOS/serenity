test("length is 2", () => {
    expect(FinalizationRegistry.prototype.unregister).toHaveLength(1);
});

test("basic functionality", () => {
    var registry = new FinalizationRegistry(() => {});

    var target = {};
    var heldValue = {};
    var token = {};

    registry.register(target, heldValue, token);

    expect(registry.unregister({})).toBe(false);
    expect(registry.unregister(token)).toBe(true);
    expect(registry.unregister(token)).toBe(false);
});

test("errors", () => {
    var registry = new FinalizationRegistry(() => {});

    expect(() => {
        registry.unregister(5);
    }).toThrowWithMessage(TypeError, "is not an object");
});
