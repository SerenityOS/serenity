test("basic functionality", () => {
    expect(String.prototype.match).toHaveLength(1);

    expect("hello friends".match(/hello/)).not.toBeNull();
    expect("hello friends".match(/enemies/)).toBeNull();

    expect("aaa".match(/a/)).toEqual(["a"]);
    expect("aaa".match(/a/g)).toEqual(["a", "a", "a"]);

    expect("aaa".match(/b/)).toBeNull();
    expect("aaa".match(/b/g)).toBeNull();
});

test("override exec with function", () => {
    let calls = 0;

    let re = /test/;
    let oldExec = re.exec.bind(re);
    re.exec = function (...args) {
        ++calls;
        return oldExec(...args);
    };

    expect("test".match(re)).not.toBeNull();
    expect(calls).toBe(1);
});

test("override exec with bad function", () => {
    let calls = 0;

    let re = /test/;
    re.exec = function (...args) {
        ++calls;
        return 4;
    };

    expect(() => {
        "test".match(re);
    }).toThrow(TypeError);
    expect(calls).toBe(1);
});

test("override exec with non-function", () => {
    let re = /test/;
    re.exec = 3;
    expect("test".match(re)).not.toBeNull();
});
