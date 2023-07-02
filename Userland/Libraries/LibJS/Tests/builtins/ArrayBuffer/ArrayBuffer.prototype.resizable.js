test("basic functionality", () => {
    expect(new ArrayBuffer().resizable).toBeFalse();
    expect(new ArrayBuffer(0, { byteLength: 16 }).resizable).toBeFalse();
    expect(new ArrayBuffer(0, { maxByteLength: 0 }).resizable).toBeTrue();
    expect(new ArrayBuffer(16, { maxByteLength: 16 }).resizable).toBeTrue();
});
