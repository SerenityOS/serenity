test("length is 1", () => {
    expect(Set.prototype.forEach).toHaveLength(1);
});

describe("errors", () => {
    test("requires at least one argument", () => {
        expect(() => {
            new Set().forEach();
        }).toThrowWithMessage(TypeError, "undefined is not a function");
    });

    test("callback must be a function", () => {
        expect(() => {
            new Set().forEach(undefined);
        }).toThrowWithMessage(TypeError, "undefined is not a function");
    });
});

describe("normal behavior", () => {
    test("never calls callback with empty set", () => {
        var callbackCalled = 0;
        expect(
            new Set().forEach(() => {
                callbackCalled++;
            })
        ).toBeUndefined();
        expect(callbackCalled).toBe(0);
    });

    test("calls callback once for every item", () => {
        var callbackCalled = 0;
        expect(
            new Set([1, 2, 3]).forEach(() => {
                callbackCalled++;
            })
        ).toBeUndefined();
        expect(callbackCalled).toBe(3);
    });

    test("callback receives value twice and set", () => {
        var a = new Set([1, 2, 3]);
        a.forEach((value1, value2, set) => {
            expect(a.has(value1)).toBeTrue();
            expect(value1).toBe(value2);
            expect(set).toBe(a);
        });
    });
});
