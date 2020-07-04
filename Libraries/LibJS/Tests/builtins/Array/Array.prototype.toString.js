test("length is 0", () => {
    expect(Array.prototype.toString).toHaveLength(0);
});

describe("normal behavior", () => {
    test("array with no elements", () => {
        expect([].toString()).toBe("");
    });

    test("array with one element", () => {
        expect([1].toString()).toBe("1");
    });

    test("array with multiple elements", () => {
        expect([1, 2, 3].toString()).toBe("1,2,3");
    });

    test("string and array concatenation", () => {
        expect("rgb(" + [10, 11, 12] + ")").toBe("rgb(10,11,12)");
    });

    test("null and undefined result in empty strings", () => {
        expect([null].toString()).toBe("");
        expect([undefined].toString()).toBe("");
        expect([undefined, null].toString()).toBe(",");
    });

    test("empty values result in empty strings", () => {
        expect(new Array(1).toString()).toBe("");
        expect(new Array(3).toString()).toBe(",,");
        var a = new Array(5);
        a[2] = "foo";
        a[4] = "bar";
        expect(a.toString()).toBe(",,foo,,bar");
    });

    test("getter property is included in returned string", () => {
        var a = [1, 2, 3];
        Object.defineProperty(a, 3, {
            get() {
                return 10;
            },
        });
        expect(a.toString()).toBe("1,2,3,10");
    });

    test("array with elements that have a custom toString() function", () => {
        var toStringCalled = 0;
        var o = {
            toString() {
                toStringCalled++;
                return "o";
            },
        };
        expect([o, undefined, o, null, o].toString()).toBe("o,,o,,o");
        expect(toStringCalled).toBe(3);
    });
});
