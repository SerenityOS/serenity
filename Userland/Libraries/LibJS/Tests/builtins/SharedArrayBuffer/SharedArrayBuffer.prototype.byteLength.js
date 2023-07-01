test("basic functionality", () => {
    expect(new SharedArrayBuffer().byteLength).toBe(0);
    expect(new SharedArrayBuffer(1).byteLength).toBe(1);
    expect(new SharedArrayBuffer(64).byteLength).toBe(64);
    expect(new SharedArrayBuffer(123).byteLength).toBe(123);
});
