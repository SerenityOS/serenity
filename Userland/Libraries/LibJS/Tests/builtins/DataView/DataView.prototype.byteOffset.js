test("basic functionality", () => {
    const buffer = new ArrayBuffer(124);
    expect(new DataView(buffer).byteOffset).toBe(0);
    expect(new DataView(buffer, 1).byteOffset).toBe(1);
    expect(new DataView(buffer, 64).byteOffset).toBe(64);
    expect(new DataView(buffer, 123).byteOffset).toBe(123);
});
