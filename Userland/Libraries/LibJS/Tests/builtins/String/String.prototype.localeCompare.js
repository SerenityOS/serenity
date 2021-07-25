test("basic functionality", () => {
    expect(String.prototype.localeCompare).toHaveLength(1);

    expect("".localeCompare("")).toBe(0);
    expect("a".localeCompare("a")).toBe(0);
    expect("6".localeCompare("6")).toBe(0);

    function compareBoth(a, b) {
        const aTob = a.localeCompare(b);
        const bToa = b.localeCompare(a);

        expect(aTob).toBe(1);
        expect(aTob).toBe(-bToa);
    }

    compareBoth("a", "");
    compareBoth("1", "");
    compareBoth("a", "A");
    compareBoth("7", "3");
    compareBoth("0000", "0");

    expect("undefined".localeCompare()).toBe(0);
    expect("undefined".localeCompare(undefined)).toBe(0);

    expect("null".localeCompare(null)).toBe(0);
    expect("null".localeCompare(undefined)).not.toBe(0);
    expect("null".localeCompare()).toBe(-1);

    expect(() => {
        String.prototype.localeCompare.call(undefined, undefined);
    }).toThrowWithMessage(TypeError, "undefined cannot be converted to an object");
});

test("UTF-16", () => {
    var s = "😀😀";
    expect(s.localeCompare("😀😀")).toBe(0);
    expect(s.localeCompare("\ud83d")).toBe(1);
    expect(s.localeCompare("😀😀s")).toBe(-1);
});
