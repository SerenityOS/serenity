test("length is 0", () => {
    expect(FinalizationRegistry.prototype.cleanupSome).toHaveLength(0);
});

function registerInDifferentScope(registry) {
    registry.register({}, {});
}

// Flaky test, investigate and fix :^)
test.skip("basic functionality", () => {
    var registry = new FinalizationRegistry(() => {});

    var count = 0;
    var increment = () => {
        count++;
    };

    registry.cleanupSome(increment);

    expect(count).toBe(0);

    registerInDifferentScope(registry);
    gc();

    registry.cleanupSome(increment);

    expect(count).toBe(1);
});

test("errors", () => {
    var registry = new FinalizationRegistry(() => {});

    expect(() => {
        registry.cleanupSome(5);
    }).toThrowWithMessage(TypeError, "is not a function");
});
