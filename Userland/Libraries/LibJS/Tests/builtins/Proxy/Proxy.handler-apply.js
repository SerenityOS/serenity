describe("[[Call]] trap normal behavior", () => {
    test("forwarding when not defined in handler", () => {
        let p = new Proxy(() => 5, { apply: null });
        expect(p()).toBe(5);
        p = new Proxy(() => 5, { apply: undefined });
        expect(p()).toBe(5);
        p = new Proxy(() => 5, {});
        expect(p()).toBe(5);
    });

    test("correct arguments supplied to trap", () => {
        const f = (a, b) => a + b;
        const handler = {
            apply(target, this_, arguments) {
                expect(target).toBe(f);
                expect(this_).toBe(handler);
                if (arguments[2]) return arguments[0] * arguments[1];
                return f(...arguments);
            },
        };
        p = new Proxy(f, handler);

        expect(p(2, 4)).toBe(6);
        expect(p(2, 4, true)).toBe(8);
    });
});

describe("[[Call]] invariants", () => {
    test("target must have a [[Call]] slot", () => {
        [{}, [], new Proxy({}, {})].forEach(item => {
            expect(() => {
                new Proxy(item, {})();
            }).toThrowWithMessage(TypeError, "[object ProxyObject] is not a function");
        });
    });
});
