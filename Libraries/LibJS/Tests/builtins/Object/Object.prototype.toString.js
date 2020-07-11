test("length", () => {
    expect(Object.prototype.toString).toHaveLength(0);
});

test("result for various object types", () => {
    const oToString = o => Object.prototype.toString.call(o);

    expect(oToString(undefined)).toBe("[object Undefined]");
    expect(oToString(null)).toBe("[object Null]");
    expect(oToString([])).toBe("[object Array]");
    expect(oToString(function () {})).toBe("[object Function]");
    expect(oToString(new Error())).toBe("[object Error]");
    expect(oToString(new Boolean())).toBe("[object Boolean]");
    expect(oToString(new Number())).toBe("[object Number]");
    expect(oToString(new Date())).toBe("[object Date]");
    expect(oToString(new RegExp())).toBe("[object RegExp]");
    expect(oToString({})).toBe("[object Object]");
});
