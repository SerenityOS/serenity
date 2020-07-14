test("length is 0", () => {
    expect(Array.prototype.toLocaleString).toHaveLength(0);
});

describe("normal behavior", () => {
    test("array with no elements", () => {
        expect([].toLocaleString()).toBe("");
    });

    test("array with one element", () => {
        expect(["foo"].toLocaleString()).toBe("foo");
    });

    test("array with multiple elements", () => {
        expect(["foo", "bar", "baz"].toLocaleString()).toBe("foo,bar,baz");
    });

    test("null and undefined result in empty strings", () => {
        expect([null].toLocaleString()).toBe("");
        expect([undefined].toLocaleString()).toBe("");
        expect([undefined, null].toLocaleString()).toBe(",");
    });

    test("empty values result in empty strings", () => {
        expect(new Array(1).toLocaleString()).toBe("");
        expect(new Array(3).toLocaleString()).toBe(",,");
        var a = new Array(5);
        a[2] = "foo";
        a[4] = "bar";
        expect(a.toLocaleString()).toBe(",,foo,,bar");
    });

    test("getter property is included in returned string", () => {
        var a = ["foo"];
        Object.defineProperty(a, 1, {
            get() {
                return "bar";
            },
        });
        expect(a.toLocaleString()).toBe("foo,bar");
    });

    test("array with elements that have a custom toString() function", () => {
        var toStringCalled = 0;
        var o = {
            toString() {
                toStringCalled++;
                return "o";
            },
        };
        expect([o, undefined, o, null, o].toLocaleString()).toBe("o,,o,,o");
        expect(toStringCalled).toBe(3);
    });
});
