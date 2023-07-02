test("basic functionality", () => {
    expect(new ArrayBuffer(0, { maxByteLength: 0 }).maxByteLength).toBe(0);
    expect(new ArrayBuffer(0, { maxByteLength: 1 }).maxByteLength).toBe(1);
    expect(new ArrayBuffer(0, { maxByteLength: 64 }).maxByteLength).toBe(64);
    expect(new ArrayBuffer(0, { maxByteLength: 128 }).maxByteLength).toBe(128);
});

test("maxByteLength returning byteLength", () => {
    expect(new ArrayBuffer(0).maxByteLength).toBe(0);
    expect(new ArrayBuffer(1).maxByteLength).toBe(1);
    expect(new ArrayBuffer(64).maxByteLength).toBe(64);
    expect(new ArrayBuffer(128).maxByteLength).toBe(128);
});

test("detached returns 0", () => {
    let source = new ArrayBuffer(8, { maxByteLength: 16 });
    let dest = source.transfer();

    expect(source.maxByteLength).toBe(0);
    expect(dest.maxByteLength).toBe(16);
});
