test("length is 0", () => {
    expect(WeakRef.prototype.deref).toHaveLength(0);
});

test("basic functionality", () => {
    var original = { a: 1 };
    var weakRef = new WeakRef(original);

    expect(weakRef.deref()).toBe(original);
});

test("kept alive for current synchronous execution sequence", () => {
    var weakRef;
    {
        weakRef = new WeakRef({ a: 1 });
    }
    weakRef.deref();
    gc();
    // This is fine 🔥
    expect(weakRef.deref()).not.toBe(undefined);
});
