test("return from try followed by finally with function call inside", () => {
    let value = (() => {
        try {
            return 1;
        } finally {
            (() => {})();
        }
    })();

    expect(value).toBe(1);
});

test("return from outer finally with nested unwind contexts", () => {
    let value = (() => {
        try {
            try {
                return 1;
            } finally {
            }
        } finally {
            return 2;
        }
    })();

    expect(value).toBe(2);
});

test("restore exception after generator yield in finally", () => {
    let generator = (function* () {
        try {
            throw new Error("foo");
        } finally {
            yield 42;
        }
    })();

    expect(generator.next().value).toBe(42);
    expect(() => generator.next()).toThrowWithMessage(Error, "foo");
    expect(generator.next().done).toBe(true);
});
