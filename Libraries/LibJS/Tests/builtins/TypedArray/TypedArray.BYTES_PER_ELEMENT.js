test("basic functionality", () => {
    expect(Uint8Array.BYTES_PER_ELEMENT).toBe(1);
    expect(Uint16Array.BYTES_PER_ELEMENT).toBe(2);
    expect(Uint32Array.BYTES_PER_ELEMENT).toBe(4);
    expect(Int8Array.BYTES_PER_ELEMENT).toBe(1);
    expect(Int16Array.BYTES_PER_ELEMENT).toBe(2);
    expect(Int32Array.BYTES_PER_ELEMENT).toBe(4);
});
