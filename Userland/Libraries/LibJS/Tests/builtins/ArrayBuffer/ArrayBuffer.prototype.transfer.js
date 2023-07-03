test("basic functionality", () => {
    const emptyArray = new ArrayBuffer(0);
    const emptyDest = emptyArray.transfer();

    expect(emptyArray.detached).toBe(true);
    expect(emptyDest.detached).toBe(false);
    expect(emptyDest.byteLength).toBe(0);
    expect(emptyDest.maxByteLength).toBe(0);

    const resizableArray = new ArrayBuffer(8, { maxByteLength: 16 });
    const fullView = new Uint8Array(resizableArray);
    fullView[0] = 1;

    const resizableDest = resizableArray.transfer();
    const fullDestView = new Uint8Array(resizableDest);

    expect(resizableArray.detached).toBe(true);
    expect(resizableDest.detached).toBe(false);
    expect(resizableDest.resizable).toBe(true);
    expect(fullView.byteLength).toBe(0);
    expect(resizableDest.byteLength).toBe(8);
    expect(resizableDest.maxByteLength).toBe(16);
    expect(fullDestView.byteLength).toBe(8);
    expect(fullDestView[0]).toBe(1);
});
