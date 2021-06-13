test("basic functionality", () => {
    const buffer = new ArrayBuffer(124);
    expect(new DataView(buffer).byteLength).toBe(124);
    expect(new DataView(buffer, 0, 1).byteLength).toBe(1);
    expect(new DataView(buffer, 0, 64).byteLength).toBe(64);
    expect(new DataView(buffer, 0, 123).byteLength).toBe(123);
});
