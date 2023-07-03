test("basic functionality", () => {
    const emptyArray = new ArrayBuffer(0);
    const emptyDest = emptyArray.transferToFixedLength();

    expect(emptyArray.detached).toBe(true);
    expect(emptyDest.detached).toBe(false);
    expect(emptyDest.byteLength).toBe(0);
    expect(emptyDest.maxByteLength).toBe(0);

    const resizableArray = new ArrayBuffer(8, { maxByteLength: 16 });
    const resizableDest = resizableArray.transferToFixedLength();

    expect(resizableArray.detached).toBe(true);
    expect(resizableDest.detached).toBe(false);
    expect(resizableDest.resizable).toBe(false);
    expect(resizableDest.byteLength).toBe(8);
    expect(resizableDest.maxByteLength).toBe(8);

    const staticArray = new ArrayBuffer(8);
    const fullView = new Uint8Array(staticArray);
    fullView[0] = 1;

    const staticDest = staticArray.transferToFixedLength();
    const fullDestView = new Uint8Array(staticDest);

    expect(staticArray.detached).toBe(true);
    expect(fullView.byteLength).toBe(0);
    expect(staticDest.detached).toBe(false);
    expect(staticDest.byteLength).toBe(8);
    expect(staticDest.maxByteLength).toBe(8);
    expect(fullDestView.byteLength).toBe(8);
    expect(fullDestView[0]).toBe(1);
});
