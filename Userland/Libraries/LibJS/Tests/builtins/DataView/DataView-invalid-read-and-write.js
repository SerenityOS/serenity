test("Issue #9336, integer overflow in get_view_value", () => {
    const dataView = new DataView(new ArrayBuffer(16));
    expect(() => {
        dataView.getUint32(0xfffffffc);
    }).toThrowWithMessage(
        RangeError,
        "Data view byte offset 4294967292 is out of range for buffer with length 16"
    );
});
