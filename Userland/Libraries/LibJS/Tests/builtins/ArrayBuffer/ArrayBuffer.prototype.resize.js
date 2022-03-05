test("length is 1", () => {
    expect(ArrayBuffer.prototype.resize).toHaveLength(1);
});

test("resize up to max", () => {
    let a = new ArrayBuffer(0, { maxByteLength: 10 });
    a.resize(10);
    expect(a.byteLength).toEqual(10);
});

test("resize less than max", () => {
    let a = new ArrayBuffer(10, { maxByteLength: 10 });
    a.resize(5);
    expect(a.byteLength).toEqual(5);
});

test("resize with negative length", () => {
    let a = new ArrayBuffer(10, { maxByteLength: 10 });
    expect(() => {
        a.resize(-1);
    }).toThrowWithMessage(
        RangeError,
        "New byte length outside range supported by ArrayBuffer instance"
    );
});

test("resize past max length", () => {
    let a = new ArrayBuffer(10, { maxByteLength: 10 });
    expect(() => {
        a.resize(11);
    }).toThrowWithMessage(
        RangeError,
        "New byte length outside range supported by ArrayBuffer instance"
    );
});
