const stackDescriptor = Object.getOwnPropertyDescriptor(Error.prototype, "stack");
const stackSetter = stackDescriptor.set;

describe("getter - normal behavior", () => {
    test("basic functionality", () => {
        const stackFrames = [
            /^    at .*Error \(.*\/Error\.prototype\.stack\.js:\d+:\d+\)$/,
            /^    at .+\/Error\/Error\.prototype\.stack\.js:\d+:\d+$/,
            /^    at test \(.+\/test-common.js:557:21\)$/,
            /^    at .+\/Error\/Error\.prototype\.stack\.js:5:33$/,
            /^    at describe \(.+\/test-common\.js:534:21\)$/,
            /^    at .+\/Error\/Error\.prototype\.stack\.js:4:38$/,
        ];
        const values = [
            {
                error: new Error(),
                header: "Error",
                stackFrames,
            },
            {
                error: new TypeError(),
                header: "TypeError",
                stackFrames,
            },
            {
                error: new Error("Something went wrong!"),
                header: "Error: Something went wrong!",
                stackFrames,
            },
        ];

        for (const { error, header: expectedHeader, stackFrames: expectedStackFrames } of values) {
            const [header, ...stackFrames] = error.stack.trim().split("\n");

            expect(header).toBe(expectedHeader);
            expect(stackFrames).toHaveLength(expectedStackFrames.length);
            for (let i = 0; i < stackFrames.length; ++i) {
                const stackFrame = stackFrames[i];
                const expectedStackFrame = expectedStackFrames[i];
                expect(!!stackFrame.match(expectedStackFrame)).toBeTrue();
            }
        }
    });
});

describe("setter - normal behavior", () => {
    test("basic functionality", () => {
        "use strict";
        const error = new Error();
        expect(Object.hasOwn(error, "stack")).toBeFalse();
        expect(() => {
            error.stack = "hello world";
        }).not.toThrowWithMessage(TypeError, "Cannot set property 'stack' of [object Error]");
        expect(Object.hasOwn(error, "stack")).toBeTrue();
        expect(error.stack).toBe("hello world");
    });

    test("the name of the set function is 'set stack'", () => {
        expect(stackSetter.name).toBe("set stack");
    });

    test("works on any object", () => {
        const nonError = {};
        expect(Object.hasOwn(nonError, "stack")).toBeFalse();
        stackSetter.call(nonError, "hello world");
        expect(Object.hasOwn(nonError, "stack")).toBeTrue();
        expect(nonError.stack).toBe("hello world");
    });

    test("accepts any value", () => {
        const nonError = { stack: 1 };
        stackSetter.call(nonError, undefined);
        expect(nonError.stack).toBeUndefined();
        stackSetter.call(nonError, null);
        expect(nonError.stack).toBeNull();
        stackSetter.call(nonError, NaN);
        expect(nonError.stack).toBeNaN();
        stackSetter.call(nonError, 1n);
        expect(nonError.stack).toBe(1n);
    });
});

describe("setter - errors", () => {
    test("this is not an object", () => {
        expect(() => {
            stackSetter.call(undefined);
        }).toThrowWithMessage(TypeError, "undefined is not an object");

        expect(() => {
            stackSetter.call(null);
        }).toThrowWithMessage(TypeError, "null is not an object");

        expect(() => {
            stackSetter.call(1);
        }).toThrowWithMessage(TypeError, "1 is not an object");

        expect(() => {
            stackSetter.call("foo");
        }).toThrowWithMessage(TypeError, "foo is not an object");
    });

    test("requires one argument", () => {
        expect(() => {
            stackSetter.call({});
        }).toThrowWithMessage(TypeError, "set stack() needs one argument");
    });

    test("create_data_property_or_throw can throw", () => {
        const revocable = Proxy.revocable([], {});
        revocable.revoke();
        expect(() => {
            stackSetter.call(revocable.proxy, "foo");
        }).toThrowWithMessage(TypeError, "An operation was performed on a revoked Proxy object");
    });
});
