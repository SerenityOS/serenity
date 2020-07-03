let describe;
let test;
let expect;

// Stores the results of each test and suite. Has a terrible
// name to avoid name collision.
let __TestResults__ = {};

// This array is used to communicate with the C++ program. It treats
// each message in this array as a separate message. Has a terrible
// name to avoid name collision.
let __UserOutput__ = [];

// We also rebind console.log here to use the array above
console.log = (...args) => {
    __UserOutput__.push(args.join(" "));
};

// Use an IIFE to avoid polluting the global namespace as much as possible
(() => {

// FIXME: This is a very naive deepEquals algorithm
const deepEquals = (a, b) => {
    if (Array.isArray(a))
        return Array.isArray(b) && deepArrayEquals(a, b);
    if (typeof a === "object")
        return typeof b === "object" && deepObjectEquals(a, b);
    return Object.is(a, b);
}

const deepArrayEquals = (a, b) => {
    if (a.length !== b.length)
        return false;
    for (let i = 0; i < a.length; ++i) {
        if (!deepEquals(a[i], b[i]))
            return false;
    }
    return true;
}

const deepObjectEquals = (a, b) => {
    if (a === null)
        return b === null;
    for (let key of Reflect.ownKeys(a)) {
        if (!deepEquals(a[key], b[key]))
            return false;
    }
    return true;
}

class ExpectationError extends Error {
    constructor(message, fileName, lineNumber) {
        super(message, fileName, lineNumber);
        this.name = "ExpectationError";
    }
}

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
            this.__expect(Object.is(this.target, value));
        });
    }

    toHaveLength(length) {
        this.__doMatcher(() => {
            this.__expect(Object.is(this.target.length, length));
        });
    }

    toHaveProperty(property, value) {
        this.__doMatcher(() => {
            let object = this.target;
    
            if (typeof property === "string" && property.includes(".")) {
                let propertyArray = [];
    
                while (true) {
                    let index = property.indexOf(".");
                    if (index === -1) {
                        propertyArray.push(property);
                        break;
                    }
    
                    propertyArray.push(property.substring(0, index));
                    property = property.substring(index, property.length);
                }
    
                property = propertyArray;
            }
    
            if (Array.isArray(property)) {
                for (let key of property) {
                    if (object === undefined || object === null) {
                        if (this.inverted)
                            return;
                        throw new ExpectationError();
                    }
                    object = object[key];
                }
            } else {
                object = object[property];
            }

            this.__expect(object !== undefined);
            if (value !== undefined)
                this.__expect(deepEquals(object, value));
        });
    }

    toBeCloseTo(number, numDigits) {
        if (numDigits === undefined)
            numDigits = 2;

        this.__doMatcher(() => {
            this.__expect(Math.abs(number - this.target) < (10 ** -numDigits / numDigits));
        });
    }

    toBeDefined() {
        this.__doMatcher(() => {
            this.__expect(this.target !== undefined);
        });
    }

    toBeFalsey() {
        this.__doMatcher(() => {
            this.__expect(!this.target);
        });
    }

    toBeGreaterThan(number) {
        this.__doMatcher(() => {
            this.__expect(this.target > number);
        });
    }

    toBeGreaterThanOrEqual(number) {
        this.__doMatcher(() => {
            this.__expect(this.target >= number);
        });
    }

    toBeLessThan(number) {
        this.__doMatcher(() => {
            this.__expect(this.target < number);
        });
    }

    toBeLessThanOrEqual(number) {
        this.__doMatcher(() => {
            this.__expect(this.target <= number);
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

    toBeTruthy() {
        this.__doMatcher(() => {
            this.__expect(!!this.target);
        });
    }

    toBeUndefined() {
        this.__doMatcher(() => {
            this.__expect(this.target === undefined);
        });
    }

    toBeNaN() {
        this.__doMatcher(() => {
            this.__expect(isNaN(this.target));
        });
    }

    toContain(item) {
        this.__doMatcher(() => {
            // FIXME: Iterator check
            for (let element of this.target) {
                if (item === element)
                    return;
            }
            
            throw new ExpectationError();
        });
    }

    toContainEqual(item) {
        this.__doMatcher(() => {
            // FIXME: Iterator check
            for (let element of this.target) {
                if (deepEquals(item, element))
                    return;
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
        this.__expect(typeof value === "string" || typeof value === "function" || value === undefined);

        this.__doMatcher(() => {
            try {
                this.target();
                this.__expect(false);
            } catch (e) {
                if (typeof value === "string") {
                    this.__expect(e.message.includes(value));
                } else if (typeof value === "function") {
                    this.__expect(e instanceof value);
                }
            }
        });
    }

    pass(message) {
        // FIXME: This does nothing. If we want to implement things
        // like assertion count, this will have to do something
    }

    // jest-extended
    fail(message) {
        // FIXME: message is currently ignored
        this.__doMatcher(() => {
            this.__expect(false);
        })
    }

    // jest-extended
    toThrowWithMessage(class_, message) {
        this.__expect(typeof this.target === "function");
        this.__expect(class_ !== undefined);
        this.__expect(message !== undefined);

        this.__doMatcher(() => {
            try {
                this.target();
                this.__expect(false);
            } catch (e) {
                this.__expect(e instanceof class_);
                this.__expect(e.message.includes(message));
            }
        });
    }

    // Test for syntax errors; target must be a string
    toEval() {
        this.__expect(typeof this.target === "string");

        if (!this.inverted) {
            try {
                new Function(this.target)();
            } catch (e) {
                throw new ExpectationError();
            }
        } else {
            try {
                new Function(this.target)();
                throw new ExpectationError();
            } catch (e) {
                if (e.name !== "SyntaxError") 
                    throw new ExpectationError();
            }
        }
    }

    // Must compile regardless of inverted-ness
    toEvalTo(value) {
        this.__expect(typeof this.target === "string");

        let result;

        try {
            result = new Function(this.target)();
        } catch (e) {
            throw new ExpectationError();
        }

        this.__doMatcher(() => {
            this.__expect(deepEquals(value, result));
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
                if (e.name === "ExpectationError")
                    threw = true;
            }
            if (!threw)
                throw new ExpectationError();
        }
    }

    __expect(value) {
        if (value !== true)
            throw new ExpectationError();
    }
}

expect = value => new Expector(value);

// describe is able to lump test results inside of it by using this context
// variable. Top level tests are assumed to be in the default context
const defaultSuiteMessage = "__$$TOP_LEVEL$$__";
let suiteMessage = defaultSuiteMessage;

describe = (message, callback) => {
    suiteMessage = message;
    callback();
    suiteMessage = defaultSuiteMessage;
}

test = (message, callback) => {
    if (!__TestResults__[suiteMessage])
        __TestResults__[suiteMessage] = {};

    const suite = __TestResults__[suiteMessage];

    if (!suite[message])
        suite[message] = {};

    try {
        callback();
        suite[message] = {
            passed: true,
        };
    } catch (e) {
        suite[message] = {
            passed: false,
        };
    }
}

})();
