test("length is 1", () => {
    expect(Array.prototype.groupByToMap).toHaveLength(1);
});

describe("errors", () => {
    test("callback must be a function", () => {
        expect(() => {
            [].groupByToMap(undefined);
        }).toThrowWithMessage(TypeError, "undefined is not a function");
    });

    test("null or undefined this value", () => {
        expect(() => {
            Array.prototype.groupByToMap.call();
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

        expect(() => {
            Array.prototype.groupByToMap.call(undefined);
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");

        expect(() => {
            Array.prototype.groupByToMap.call(null);
        }).toThrowWithMessage(TypeError, "ToObject on null or undefined");
    });
});

describe("normal behavior", () => {
    test("basic functionality", () => {
        const array = [1, 2, 3, 4, 5, 6];
        const visited = [];
        const trueObject = { true: true };
        const falseObject = { false: false };

        const firstResult = array.groupByToMap(value => {
            visited.push(value);
            return value % 2 === 0 ? trueObject : falseObject;
        });

        expect(visited).toEqual([1, 2, 3, 4, 5, 6]);
        expect(firstResult).toBeInstanceOf(Map);
        expect(firstResult.size).toBe(2);
        expect(firstResult.get(trueObject)).toEqual([2, 4, 6]);
        expect(firstResult.get(falseObject)).toEqual([1, 3, 5]);

        const secondResult = array.groupByToMap((_, index) => {
            return index < array.length / 2 ? trueObject : falseObject;
        });

        expect(secondResult).toBeInstanceOf(Map);
        expect(secondResult.size).toBe(2);
        expect(secondResult.get(trueObject)).toEqual([1, 2, 3]);
        expect(secondResult.get(falseObject)).toEqual([4, 5, 6]);

        const thisArg = [7, 8, 9, 10, 11, 12];
        const thirdResult = array.groupByToMap(function (_, __, arrayVisited) {
            expect(arrayVisited).toBe(array);
            expect(this).toBe(thisArg);
        }, thisArg);

        expect(thirdResult).toBeInstanceOf(Map);
        expect(thirdResult.size).toBe(1);
        expect(thirdResult.get(undefined)).not.toBe(array);
        expect(thirdResult.get(undefined)).not.toBe(thisArg);
        expect(thirdResult.get(undefined)).toEqual(array);
    });

    test("is unscopable", () => {
        expect(Array.prototype[Symbol.unscopables].groupByToMap).toBeTrue();
        const array = [];
        with (array) {
            expect(() => {
                groupByToMap;
            }).toThrowWithMessage(ReferenceError, "'groupByToMap' is not defined");
        }
    });

    test("never calls callback with empty array", () => {
        var callbackCalled = 0;
        const result = [].groupByToMap(() => {
            callbackCalled++;
        });
        expect(result).toBeInstanceOf(Map);
        expect(result.size).toBe(0);
        expect(callbackCalled).toBe(0);
    });

    test("calls callback once for every item", () => {
        var callbackCalled = 0;
        const result = [1, 2, 3].groupByToMap(() => {
            callbackCalled++;
        });
        expect(result).toBeInstanceOf(Map);
        expect(result.size).toBe(1);
        expect(result.get(undefined)).toEqual([1, 2, 3]);
        expect(callbackCalled).toBe(3);
    });

    test("still returns a Map even if the global Map constructor was changed", () => {
        globalThis.Map = null;
        const result = [1, 2].groupByToMap(value => {
            return value % 2 === 0;
        });
        expect(result.size).toBe(2);
        expect(result.get(true)).toEqual([2]);
        expect(result.get(false)).toEqual([1]);
    });
});
