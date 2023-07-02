test("invalid maxByteLength throws", () => {
    expect(() => {
        new ArrayBuffer(8, { maxByteLength: 4 });
    }).toThrowWithMessage(
        RangeError,
        "Invalid byte length for ArrayBuffer 8 when the max byte length is 4"
    );

    expect(() => {
        new ArrayBuffer(8, { maxByteLength: -1 });
    }).toThrowWithMessage(RangeError, "Index must be a positive integer");
});
