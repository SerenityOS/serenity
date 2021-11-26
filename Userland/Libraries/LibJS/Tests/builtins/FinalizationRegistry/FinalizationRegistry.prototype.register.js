test("length is 2", () => {
    expect(FinalizationRegistry.prototype.register).toHaveLength(2);
});

test("basic functionality", () => {
    var registry = new FinalizationRegistry(() => {});

    var target1 = {};
    var heldValue1 = {};

    registry.register(target1, heldValue1);

    var target2 = {};
    var heldValue2 = {};
    var token = {};

    registry.register(target2, heldValue2, token);
});

test("errors", () => {
    var registry = new FinalizationRegistry(() => {});

    expect(() => {
        registry.register(5, {});
    }).toThrowWithMessage(TypeError, "is not an object");

    expect(() => {
        var a = {};
        registry.register(a, a);
    }).toThrowWithMessage(TypeError, "Target and held value must not be the same");

    expect(() => {
        registry.register({}, {}, 5);
    }).toThrowWithMessage(TypeError, "is not an object");
});
