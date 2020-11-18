test("basic functionality", () => {
    expect(RegExp.prototype.test).toHaveLength(1);
});

test("simple test", () => {
    let re = /test/;
    expect(re.test("test")).toBe(true);
    expect(re.test("test")).toBe(true);
});

test("simple global test", () => {
    let re = /test/g;
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(false);
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(true);
});

test("global test with offset lastIndex", () => {
    let re = /test/g;
    re.lastIndex = 2;
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(false);
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(true);
    expect(re.test("testtest")).toBe(false);
});

test("sticky test with offset lastIndex", () => {
    let re = /test/y;
    re.lastIndex = 2;
    expect(re.test("aatest")).toBe(true);
    expect(re.test("aatest")).toBe(false);
    expect(re.test("aatest")).toBe(false);
});

test("flag and options", () => {
    expect(/foo/gi.flags).toBe("gi");
    expect(/foo/mu.flags).toBe("mu");
    expect(/foo/gimsuy.flags).toBe("gimsuy");

    let re = /foo/gim;
    expect(re.dotAll).toBe(false);
    expect(re.global).toBe(true);
    expect(re.ignoreCase).toBe(true);
    expect(re.multiline).toBe(true);
    expect(re.sticky).toBe(false);
    expect(re.unicode).toBe(false);

    expect(() => {
        /foo/gg;
    }).toThrowWithMessage(SyntaxError, "Repeated RegExp flag 'g'");

    expect(() => {
        /foo/x;
    }).toThrowWithMessage(SyntaxError, "Invalid RegExp flag 'x'");
});
