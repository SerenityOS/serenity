test("single parameter", () => {
    const buffer = new SharedArrayBuffer(16);
    const fullView = new Int32Array(buffer);

    // modify some value that we can check in the sliced buffer
    fullView[3] = 7;

    // slice the buffer and use a new int32 view to perform basic checks
    const slicedBuffer = buffer.slice(12);
    const slicedView = new Int32Array(slicedBuffer);

    expect(slicedView).toHaveLength(1);
    expect(slicedView[0]).toBe(7);
});

test("both parameters", () => {
    const buffer = new SharedArrayBuffer(16);
    const fullView = new Int32Array(buffer);

    // modify some value that we can check in the sliced buffer
    fullView[1] = 12;

    // slice the buffer and use a new int32 view to perform basic checks
    const slicedBuffer = buffer.slice(4, 8);
    const slicedView = new Int32Array(slicedBuffer);

    expect(slicedView).toHaveLength(1);
    expect(slicedView[0]).toBe(12);
});

test("throws TypeError if |this| is ArrayBuffer", () => {
    const ab = new ArrayBuffer(16);
    expect(() => SharedArrayBuffer.prototype.slice.call(ab)).toThrow(TypeError);
});

test("slice creates a new SharedArrayBuffer", () => {
    const sab = new SharedArrayBuffer(16);
    expect(sab).toBeInstanceOf(SharedArrayBuffer);
    expect(sab.slice(0)).toBeInstanceOf(SharedArrayBuffer);
});
