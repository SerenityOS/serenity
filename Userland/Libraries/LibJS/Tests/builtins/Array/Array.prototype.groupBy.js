test("length is 1", () => {
    expect(Array.prototype.groupBy).toHaveLength(1);
});

describe("errors", () => {
    test("callback must be a function", () => {
        expect(() => {
            [].groupBy(undefined);
        }).toThrowWithMessage(TypeError, "undefined is not a function");
    });

    test("null or undefined this value", () => {
        expect(() => {
            Array.prototype.groupBy.call();
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

        expect(() => {
            Array.prototype.groupBy.call(undefined);
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

        expect(() => {
            Array.prototype.groupBy.call(null);
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        const array = [1, 2, 3, 4, 5, 6];
        const visited = [];

        const firstResult = array.groupBy(value => {
            visited.push(value);
            return value % 2 === 0;
        });

        expect(visited).toEqual([1, 2, 3, 4, 5, 6]);
        expect(firstResult.true).toEqual([2, 4, 6]);
        expect(firstResult.false).toEqual([1, 3, 5]);

        const firstKeys = Object.keys(firstResult);
        expect(firstKeys).toHaveLength(2);
        expect(firstKeys[0]).toBe("false");
        expect(firstKeys[1]).toBe("true");

        const secondResult = array.groupBy((_, index) => {
            return index < array.length / 2;
        });

        expect(secondResult.true).toEqual([1, 2, 3]);
        expect(secondResult.false).toEqual([4, 5, 6]);

        const secondKeys = Object.keys(secondResult);
        expect(secondKeys).toHaveLength(2);
        expect(secondKeys[0]).toBe("true");
        expect(secondKeys[1]).toBe("false");

        const thisArg = [7, 8, 9, 10, 11, 12];
        const thirdResult = array.groupBy(function (_, __, arrayVisited) {
            expect(arrayVisited).toBe(array);
            expect(this).toBe(thisArg);
        }, thisArg);

        expect(thirdResult.undefined).not.toBe(array);
        expect(thirdResult.undefined).not.toBe(thisArg);
        expect(thirdResult.undefined).toEqual(array);

        const thirdKeys = Object.keys(thirdResult);
        expect(thirdKeys).toHaveLength(1);
        expect(thirdKeys[0]).toBe("undefined");
    });

    test("is unscopable", () => {
        expect(Array.prototype[Symbol.unscopables].groupBy).toBeTrue();
        const array = [];
        with (array) {
            expect(() => {
                groupBy;
            }).toThrowWithMessage(ReferenceError, "'groupBy' is not defined");
        }
    });

    test("never calls callback with empty array", () => {
        var callbackCalled = 0;
        expect(
            [].groupBy(() => {
                callbackCalled++;
            })
        ).toEqual({});
        expect(callbackCalled).toBe(0);
    });

    test("calls callback once for every item", () => {
        var callbackCalled = 0;
        const result = [1, 2, 3].groupBy(() => {
            callbackCalled++;
        });
        expect(result.undefined).toEqual([1, 2, 3]);
        expect(callbackCalled).toBe(3);
    });
});
