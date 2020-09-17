describe("reference errors", () => {
    test("non-existent root", () => {
        expect(() => {
            a?.b;
        }).toThrow(ReferenceError);
        expect(() => {
            a?.["b"];
        }).toThrow(ReferenceError);
    });
});

describe("syntax errors", () => {
    test("optional chaining in LHS of assignment expression", () => {
        expect("a?.b = 1").not.toEval();
        expect("a?.b() = 1").not.toEval();
        expect("a.b?.() = 1").not.toEval();
        expect("a.b?.['c']?.d?.() = 1").not.toEval();
    });

    test("optional chaining in RHS of new expression", () => {
        expect("new a?.b").not.toEval();
        expect("new a?.b()").not.toEval();
        expect("new a.b?.()").not.toEval();
        expect("new a.b?.['c']?.d").not.toEval();
    });

    test("optional chaining in tagged template", () => {
        expect("a?.b``").not.toEval();
        expect("a?.b()``").not.toEval();
        expect("a.b?.()``").not.toEval();
    });
});

describe("optional member expression", () => {
    test("null root evaluates to undefined", () => {
        const a = null;
        expect(a?.b).toBeUndefined();
        expect(a?.["b"]).toBeUndefined();
    });

    test("undefined root evaluates to undefined", () => {
        const a = undefined;
        expect(a?.b).toBeUndefined();
        expect(a?.["b"]).toBeUndefined();
    });

    test("non-nullish root evaluates normally", () => {
        const a = { b: 1 };
        expect(a?.b).toBe(1);
        expect(a?.["b"]).toBe(1);
    });

    test("nullish root short-circuits evaluation", () => {
        const a = null;
        expect(a?.b.c.d.e).toBeUndefined();
        expect(a?.["b"]["c"]["d"]["e"]).toBeUndefined();
        let i = 0;
        a?.[i++];
        expect(i).toBe(0);
    });

    test("nullish member short-circuits evaluation", () => {
        const a = {};
        expect(a.b?.c.d.e).toBeUndefined();
        expect(a["b"]?.["c"]["d"]["e"]).toBeUndefined();
        let i = 0;
        a.b?.[i++];
        a["b"]?.[i++];
        expect(i).toBe(0);
    });

    // test("non-nullish optional member expression doesn't affect regular member expression further down the chain", () => {
    //     const a = { b: null };
    //     expect(() => { a?.b.c; }).toThrow(TypeError);
    //     expect(() => { a?.b(); }).toThrow(TypeError);
    // });

    test("optional chaining stacking", () => {
        const a = { b: { c: { d: null } } };
        expect(a.b?.c.d?.e).toBeUndefined();
    });

    test("optional deletion", () => {
        const a = { b: 1 };
        expect(a.b).toBe(1);
        expect(delete a?.b).toBeTrue();
        expect(a.b).toBeUndefined();
        expect(delete a?.b).toBeTrue();
        expect(a.b).toBeUndefined();
    });
});

describe("optional call expression", () => {
    test("null root evaluates to undefined", () => {
        const a = null;
        expect(a?.b()).toBeUndefined();
        expect(a?.["b"]()).toBeUndefined();
    });

    test("undefined root evaluates to undefined", () => {
        const a = undefined;
        expect(a?.b()).toBeUndefined();
        expect(a?.["b"]()).toBeUndefined();
    });

    test("object root evaluates normally", () => {
        const a = {
            b() {
                return 1;
            },
        };
        expect(a?.b()).toBe(1);
        expect(a?.["b"]()).toBe(1);
    });

    test("nullish root short-circuits evaluation", () => {
        const a = null;
        expect(a?.b.c.d.e()).toBeUndefined();
        expect(a?.["b"]["c"]["d"]["e"]()).toBeUndefined();
        expect(a?.b(expect().fail())).toBeUndefined();
        expect(a?.["b"](expect().fail())).toBeUndefined();
    });

    test("nullish member short-circuits evaluation", () => {
        const a = {};
        expect(a.b?.c.d.e()).toBeUndefined();
        expect(a["b"]?.["c"]["d"]["e"]()).toBeUndefined();
        expect(a.b?.(expect().fail())).toBeUndefined();
        expect(a["b"]?.(expect().fail())).toBeUndefined();
    });

    test("multiple optional chaining operators in one expression", () => {
        const a = { b: { c: { d: null } } };
        expect(a.b?.c.d?.()?.e?.()).toBeUndefined();
    });

    test("this value is computed and set correctly", () => {
        const o = {
            x: 0,
            f() {
                this.x++;
            }
        };
        o?.f?.();
        expect(o.x).toBe(1);
    });
});

test("advanced expressions with optional chaining", () => {
    const a = null;
    expect(a?.b.c).toBeUndefined();
    expect(a?.b["c"]).toBeUndefined();
    expect((a?.b)?.c).toBeUndefined();
    expect((a?.b)?.["c"]).toBeUndefined();
    expect(a?.[(a.b[(a.b?.c)])]).toBeUndefined();
    expect(a?.[(a.b[(a.b?.["c"])])]).toBeUndefined();
    expect((a?.b)?.c()()).toBeUndefined();
    expect((a?.b)?.["c"]()()).toBeUndefined();
    expect((a?.())?.b?.()().c).toBeUndefined();
    expect((a?.())?.b?.()()["c"]).toBeUndefined();
    expect(() => { (a.b)?.c }).toThrow(TypeError);
    expect(() => { (a.b)?.["c"] }).toThrow(TypeError);
    expect(() => { (a?.b).c }).toThrow(TypeError);
    expect(() => { (a?.b)["c"] }).toThrow(TypeError);
    expect(() => { a.b?.c }).toThrow(TypeError);
    expect(() => { a.b?.["c"] }).toThrow(TypeError);
    expect(() => { a.b?.c() }).toThrow(TypeError);
    expect(() => { a.b?.["c"]() }).toThrow(TypeError);
});
