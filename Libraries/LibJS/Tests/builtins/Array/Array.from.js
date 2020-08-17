test("length is 1", () => {
    expect(Array.from).toHaveLength(1);
});

describe("normal behavior", () => {
    test("empty array", () => {
        var a = Array.from([]);
        expect(a instanceof Array).toBeTrue();
        expect(a).toHaveLength(0);
    });

    test("empty string", () => {
        var a = Array.from("");
        expect(a instanceof Array).toBeTrue();
        expect(a).toHaveLength(0);
    });

    test("non-empty array", () => {
        var a = Array.from([5, 8, 1]);
        expect(a instanceof Array).toBeTrue();
        expect(a).toHaveLength(3);
        expect(a[0]).toBe(5);
        expect(a[1]).toBe(8);
        expect(a[2]).toBe(1);
    });

    test("non-empty string", () => {
        var a = Array.from("what");
        expect(a instanceof Array).toBeTrue();
        expect(a).toHaveLength(4);
        expect(a[0]).toBe("w");
        expect(a[1]).toBe("h");
        expect(a[2]).toBe("a");
        expect(a[3]).toBe("t");
    });

    test("shallow array copy", () => {
        var a = [1, 2, 3];
        var b = Array.from([a]);
        expect(b instanceof Array).toBeTrue();
        expect(b).toHaveLength(1);
        b[0][0] = 4;
        expect(a[0]).toBe(4);
    });

    test("from iterator", () => {
        function rangeIterator(begin, end) {
            return {
                [Symbol.iterator]() {
                    let value = begin - 1;
                    return {
                        next() {
                            if (value < end)
                                value += 1;
                            return { value: value, done: value >= end };
                        },
                    };
                },
            };
        }

        var a = Array.from(rangeIterator(8, 10));
        expect(a instanceof Array).toBeTrue();
        expect(a).toHaveLength(2);
        expect(a[0]).toBe(8);
        expect(a[1]).toBe(9);
    });
});
