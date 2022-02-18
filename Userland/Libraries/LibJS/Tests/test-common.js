var describe;
var test;
var expect;

// Stores the results of each test and suite. Has a terrible
// name to avoid name collision.
var __TestResults__ = {};

// So test names like "toString" don't automatically produce an error
Object.setPrototypeOf(__TestResults__, null);

// This array is used to communicate with the C++ program. It treats
// each message in this array as a separate message. Has a terrible
// name to avoid name collision.
var __UserOutput__ = [];

// We also rebind console.log here to use the array above
console.log = (...args) => {
    __UserOutput__.push(args.join(" "));
};

class ExpectationError extends Error {
    constructor(message) {
        super(message);
        this.name = "ExpectationError";
    }
}

// Use an IIFE to avoid polluting the global namespace as much as possible
(() => {
    // FIXME: This is a very naive deepEquals algorithm
    const deepEquals = (a, b) => {
        if (Array.isArray(a)) return Array.isArray(b) && deepArrayEquals(a, b);
        if (typeof a === "object") return typeof b === "object" && deepObjectEquals(a, b);
        return Object.is(a, b);
    };

    const deepArrayEquals = (a, b) => {
        if (a.length !== b.length) return false;
        for (let i = 0; i < a.length; ++i) {
            if (!deepEquals(a[i], b[i])) return false;
        }
        return true;
    };

    const deepObjectEquals = (a, b) => {
        if (a === null) return b === null;
        for (let key of Reflect.ownKeys(a)) {
            if (!deepEquals(a[key], b[key])) return false;
        }
        return true;
    };

    const valueToString = value => {
        try {
            if (value === 0 && 1 / value < 0) {
                return "-0";
            }
            return String(value);
        } catch {
            // e.g for objects without a prototype, the above throws.
            return Object.prototype.toString.call(value);
        }
    };

    class Expector {
        constructor(target, inverted) {
            this.target = target;
            this.inverted = !!inverted;
        }

        get not() {
            return new Expector(this.target, !this.inverted);
        }

        toBe(value) {
            this.__doMatcher(() => {
                this.__expect(
                    Object.is(this.target, value),
                    () =>
                        `toBe: expected _${valueToString(value)}_, got _${valueToString(
                            this.target
                        )}_`
                );
            });
        }

        // FIXME: Take a precision argument like jest's toBeCloseTo matcher
        toBeCloseTo(value) {
            this.__expect(
                typeof this.target === "number",
                () => `toBeCloseTo: expected target of type number, got ${typeof value}`
            );
            this.__expect(
                typeof value === "number",
                () => `toBeCloseTo: expected argument of type number, got ${typeof value}`
            );

            this.__doMatcher(() => {
                this.__expect(Math.abs(this.target - value) < 0.000001);
            });
        }

        toHaveLength(length) {
            this.__expect(
                typeof this.target.length === "number",
                () => "toHaveLength: target.length not of type number"
            );

            this.__doMatcher(() => {
                this.__expect(Object.is(this.target.length, length));
            });
        }

        toHaveSize(size) {
            this.__expect(
                typeof this.target.size === "number",
                () => "toHaveSize: target.size not of type number"
            );

            this.__doMatcher(() => {
                this.__expect(Object.is(this.target.size, size));
            });
        }

        toHaveProperty(property, value) {
            this.__doMatcher(() => {
                let object = this.target;

                if (typeof property === "string" && property.includes(".")) {
                    let propertyArray = [];

                    while (property.includes(".")) {
                        let index = property.indexOf(".");
                        propertyArray.push(property.substring(0, index));
                        if (index + 1 >= property.length) break;
                        property = property.substring(index + 1, property.length);
                    }

                    propertyArray.push(property);

                    property = propertyArray;
                }

                if (Array.isArray(property)) {
                    for (let key of property) {
                        this.__expect(object !== undefined && object !== null);
                        object = object[key];
                    }
                } else {
                    object = object[property];
                }

                this.__expect(object !== undefined);
                if (value !== undefined) this.__expect(deepEquals(object, value));
            });
        }

        toBeDefined() {
            this.__doMatcher(() => {
                this.__expect(
                    this.target !== undefined,
                    () => "toBeDefined: expected target to be defined, got undefined"
                );
            });
        }

        toBeInstanceOf(class_) {
            this.__doMatcher(() => {
                this.__expect(this.target instanceof class_);
            });
        }

        toBeNull() {
            this.__doMatcher(() => {
                this.__expect(this.target === null);
            });
        }

        toBeUndefined() {
            this.__doMatcher(() => {
                this.__expect(
                    this.target === undefined,
                    () =>
                        `toBeUndefined: expected target to be undefined, got _${valueToString(
                            this.target
                        )}_`
                );
            });
        }

        toBeNaN() {
            this.__doMatcher(() => {
                this.__expect(
                    isNaN(this.target),
                    () => `toBeNaN: expected target to be NaN, got _${valueToString(this.target)}_`
                );
            });
        }

        toBeTrue() {
            this.__doMatcher(() => {
                this.__expect(
                    this.target === true,
                    () =>
                        `toBeTrue: expected target to be true, got _${valueToString(this.target)}_`
                );
            });
        }

        toBeFalse() {
            this.__doMatcher(() => {
                this.__expect(
                    this.target === false,
                    () =>
                        `toBeFalse: expected target to be false, got _${valueToString(
                            this.target
                        )}_`
                );
            });
        }

        __validateNumericComparisonTypes(value) {
            this.__expect(typeof this.target === "number" || typeof this.target === "bigint");
            this.__expect(typeof value === "number" || typeof value === "bigint");
            this.__expect(typeof this.target === typeof value);
        }

        toBeLessThan(value) {
            this.__validateNumericComparisonTypes(value);

            this.__doMatcher(() => {
                this.__expect(this.target < value);
            });
        }

        toBeLessThanOrEqual(value) {
            this.__validateNumericComparisonTypes(value);

            this.__doMatcher(() => {
                this.__expect(this.target <= value);
            });
        }

        toBeGreaterThan(value) {
            this.__validateNumericComparisonTypes(value);

            this.__doMatcher(() => {
                this.__expect(this.target > value);
            });
        }

        toBeGreaterThanOrEqual(value) {
            this.__validateNumericComparisonTypes(value);

            this.__doMatcher(() => {
                this.__expect(this.target >= value);
            });
        }

        toContain(item) {
            this.__doMatcher(() => {
                for (let element of this.target) {
                    if (item === element) return;
                }

                throw new ExpectationError();
            });
        }

        toContainEqual(item) {
            this.__doMatcher(() => {
                for (let element of this.target) {
                    if (deepEquals(item, element)) return;
                }

                throw new ExpectationError();
            });
        }

        toEqual(value) {
            this.__doMatcher(() => {
                this.__expect(deepEquals(this.target, value));
            });
        }

        toThrow(value) {
            this.__expect(typeof this.target === "function");
            this.__expect(
                typeof value === "string" ||
                    typeof value === "function" ||
                    typeof value === "object" ||
                    value === undefined
            );

            this.__doMatcher(() => {
                let threw = true;
                try {
                    this.target();
                    threw = false;
                } catch (e) {
                    if (typeof value === "string") {
                        this.__expect(e.message.includes(value));
                    } else if (typeof value === "function") {
                        this.__expect(e instanceof value);
                    } else if (typeof value === "object") {
                        this.__expect(e.message === value.message);
                    }
                }
                this.__expect(threw);
            });
        }

        pass(message) {
            // FIXME: This does nothing. If we want to implement things
            // like assertion count, this will have to do something
        }

        // jest-extended
        fail(message) {
            this.__doMatcher(() => {
                this.__expect(false, message);
            });
        }

        // jest-extended
        toThrowWithMessage(class_, message) {
            this.__expect(typeof this.target === "function");
            this.__expect(class_ !== undefined);
            this.__expect(message !== undefined);

            this.__doMatcher(() => {
                try {
                    this.target();
                    this.__expect(false, () => "toThrowWithMessage: target function did not throw");
                } catch (e) {
                    this.__expect(
                        e instanceof class_,
                        () =>
                            `toThrowWithMessage: expected error to be instance of ${valueToString(
                                class_.name
                            )}, got ${valueToString(e.name)}`
                    );
                    this.__expect(
                        e.message.includes(message),
                        () =>
                            `toThrowWithMessage: expected error message to include _${valueToString(
                                message
                            )}_, got _${valueToString(e.message)}_`
                    );
                }
            });
        }

        // Test for syntax errors; target must be a string
        toEval() {
            this.__expect(typeof this.target === "string");
            const success = canParseSource(this.target);
            this.__expect(
                this.inverted ? !success : success,
                () =>
                    `Expected _${valueToString(this.target)}_` +
                    (this.inverted ? "not to eval but it did" : "to eval but it didn't")
            );
        }

        // Must compile regardless of inverted-ness
        toEvalTo(value) {
            this.__expect(typeof this.target === "string");

            let result;

            try {
                result = eval(this.target);
            } catch (e) {
                throw new ExpectationError(
                    `Expected _${valueToString(this.target)}_ to eval but it failed with ${e}`
                );
            }

            this.__doMatcher(() => {
                this.__expect(
                    deepEquals(value, result),
                    () =>
                        `Expected _${valueToString(this.target)}_ to eval to ` +
                        `_${valueToString(value)}_ but got _${valueToString(result)}_`
                );
            });
        }

        toHaveConfigurableProperty(property) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.configurable);
            });
        }

        toHaveEnumerableProperty(property) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.enumerable);
            });
        }

        toHaveWritableProperty(property) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.writable);
            });
        }

        toHaveValueProperty(property, value) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.value !== undefined);
                if (value !== undefined) this.__expect(deepEquals(value, d.value));
            });
        }

        toHaveGetterProperty(property) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.get !== undefined);
            });
        }

        toHaveSetterProperty(property) {
            this.__expect(this.target !== undefined && this.target !== null);
            let d = Object.getOwnPropertyDescriptor(this.target, property);
            this.__expect(d !== undefined);

            this.__doMatcher(() => {
                this.__expect(d.set !== undefined);
            });
        }

        toBeIteratorResultWithValue(value) {
            this.__expect(this.target !== undefined && this.target !== null);
            this.__doMatcher(() => {
                this.__expect(
                    this.target.done === false,
                    () =>
                        `toGiveIteratorResultWithValue: expected 'done' to be _false_ got ${valueToString(
                            this.target.done
                        )}`
                );
                this.__expect(
                    deepEquals(value, this.target.value),
                    () =>
                        `toGiveIteratorResultWithValue: expected 'value' to be _${valueToString(
                            value
                        )}_ got ${valueToString(this.target.value)}`
                );
            });
        }

        toBeIteratorResultDone() {
            this.__expect(this.target !== undefined && this.target !== null);
            this.__doMatcher(() => {
                this.__expect(
                    this.target.done === true,
                    () =>
                        `toGiveIteratorResultDone: expected 'done' to be _true_ got ${valueToString(
                            this.target.done
                        )}`
                );
                this.__expect(
                    this.target.value === undefined,
                    () =>
                        `toGiveIteratorResultDone: expected 'value' to be _undefined_ got ${valueToString(
                            this.target.value
                        )}`
                );
            });
        }

        __doMatcher(matcher) {
            if (!this.inverted) {
                matcher();
            } else {
                let threw = false;
                try {
                    matcher();
                } catch (e) {
                    if (e.name === "ExpectationError") threw = true;
                }
                if (!threw) throw new ExpectationError("not: test didn't fail");
            }
        }

        __expect(value, details) {
            if (value !== true) {
                if (details !== undefined) {
                    throw new ExpectationError(details());
                } else {
                    throw new ExpectationError();
                }
            }
        }
    }

    expect = value => new Expector(value);

    // describe is able to lump test results inside of it by using this context
    // variable. Top level tests have the default suite message
    const defaultSuiteMessage = "__$$TOP_LEVEL$$__";
    let suiteMessage = defaultSuiteMessage;

    describe = (message, callback) => {
        suiteMessage = message;
        if (!__TestResults__[suiteMessage]) __TestResults__[suiteMessage] = {};
        try {
            callback();
        } catch (e) {
            __TestResults__[suiteMessage][defaultSuiteMessage] = {
                result: "fail",
                details: String(e),
            };
        }
        suiteMessage = defaultSuiteMessage;
    };

    test = (message, callback) => {
        if (!__TestResults__[suiteMessage]) __TestResults__[suiteMessage] = {};

        const suite = __TestResults__[suiteMessage];
        if (Object.prototype.hasOwnProperty.call(suite, message)) {
            suite[message] = {
                result: "fail",
                details: "Another test with the same message did already run",
            };
            return;
        }

        try {
            callback();
            suite[message] = {
                result: "pass",
            };
        } catch (e) {
            suite[message] = {
                result: "fail",
                details: String(e),
            };
        }
    };

    test.skip = (message, callback) => {
        if (typeof callback !== "function")
            throw new Error("test.skip has invalid second argument (must be a function)");

        if (!__TestResults__[suiteMessage]) __TestResults__[suiteMessage] = {};

        const suite = __TestResults__[suiteMessage];
        if (Object.prototype.hasOwnProperty.call(suite, message)) {
            suite[message] = {
                result: "fail",
                details: "Another test with the same message did already run",
            };
            return;
        }

        suite[message] = {
            result: "skip",
        };
    };
})();
