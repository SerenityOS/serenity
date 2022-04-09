test("method inheritance", () => {
    class Parent {
        method() {
            return 3;
        }
    }

    class Child extends Parent {}

    const p = new Parent();
    const c = new Child();
    expect(p.method()).toBe(3);
    expect(c.method()).toBe(3);
});

test("method overriding", () => {
    class Parent {
        method() {
            return 3;
        }
    }

    class Child extends Parent {
        method() {
            return 10;
        }
    }

    const p = new Parent();
    const c = new Child();
    expect(p.method()).toBe(3);
    expect(c.method()).toBe(10);
});

test("parent method reference with super", () => {
    class Parent {
        method() {
            return 3;
        }
    }

    class Child extends Parent {
        method() {
            return super.method() * 2;
        }
    }

    const p = new Parent();
    const c = new Child();
    expect(p.method()).toBe(3);
    expect(c.method()).toBe(6);
});

test("child class access to parent class initialized properties", () => {
    class Parent {
        constructor() {
            this.x = 3;
        }
    }

    class Child extends Parent {}

    const p = new Parent();
    const c = new Child();
    expect(p.x).toBe(3);
    expect(c.x).toBe(3);
});

test("child class modification of parent class properties", () => {
    class Parent {
        constructor() {
            this.x = 3;
        }
    }

    class Child extends Parent {
        change() {
            this.x = 10;
        }
    }

    const p = new Parent();
    const c = new Child();
    expect(p.x).toBe(3);
    expect(c.x).toBe(3);

    c.change();
    expect(c.x).toBe(10);
});

test("inheritance and hasOwnProperty", () => {
    class Parent {
        constructor() {
            this.x = 3;
        }
    }

    class Child extends Parent {
        method() {
            this.y = 10;
        }
    }

    const p = new Parent();
    const c = new Child();
    expect(p.hasOwnProperty("x")).toBeTrue();
    expect(p.hasOwnProperty("y")).toBeFalse();
    expect(c.hasOwnProperty("x")).toBeTrue();
    expect(c.hasOwnProperty("y")).toBeFalse();

    c.method();
    expect(c.hasOwnProperty("x")).toBeTrue();
    expect(c.hasOwnProperty("y")).toBeTrue();
});

test("super constructor call from child class with argument", () => {
    class Parent {
        constructor(x) {
            this.x = x;
        }
    }

    class Child extends Parent {
        constructor() {
            super(10);
        }
    }

    const p = new Parent(3);
    const c = new Child(3);
    expect(p.x).toBe(3);
    expect(c.x).toBe(10);
});

test("advanced 'extends' RHS", () => {
    const foo = {
        bar() {
            return {
                baz() {
                    return function () {
                        return function () {
                            return { quux: Number };
                        };
                    };
                },
            };
        },
    };
    class Foo extends foo.bar()["baz"]()`qux`().quux {}
    expect(new Foo()).toBeInstanceOf(Number);
});

test("issue #7045, super constructor call from child class in catch {}", () => {
    class Parent {
        constructor(x) {
            this.x = x;
        }
    }

    class Child extends Parent {
        constructor() {
            try {
                throw new Error("Error in Child constructor");
            } catch (e) {
                super(e.message);
            }
        }
    }

    const c = new Child();
    expect(c.x).toBe("Error in Child constructor");
});

test("Issue #7044, super property access before super() call", () => {
    class Foo {
        constructor() {
            super.bar;
        }
    }

    new Foo();
});

test("Issue #8574, super property access before super() call", () => {
    var hit = false;

    class Foo extends Object {
        constructor() {
            expect(() => {
                const foo = super.bar();
            }).toThrowWithMessage(ReferenceError, "|this| has not been initialized");
            hit = true;
        }
    }

    // Note: We catch two exceptions here.
    expect(() => {
        new Foo();
    }).toThrowWithMessage(ReferenceError, "|this| has not been initialized");
    expect(hit).toBeTrue();
});

test("can access super via direct eval", () => {
    let superCalled = false;
    const aObject = { a: 1 };
    const bObject = { b: 2 };

    class A {
        constructor() {
            superCalled = true;
        }

        foo() {
            return aObject;
        }

        bar() {
            return bObject;
        }
    }

    class B extends A {
        constructor() {
            eval("super()");
        }
    }

    expect(() => {
        new B();
    }).not.toThrow();

    expect(superCalled).toBeTrue();
    superCalled = false;

    class C extends A {
        constructor() {
            eval("super()");
            return eval("super.foo()");
        }
    }

    expect(() => {
        new C();
    }).not.toThrow();

    expect(superCalled).toBeTrue();
    superCalled = false;

    expect(new C()).toBe(aObject);

    expect(superCalled).toBeTrue();
    superCalled = false;

    class D extends A {
        constructor() {
            eval("super()");
            return eval("super['bar']()");
        }
    }

    expect(() => {
        new D();
    }).not.toThrow();

    expect(superCalled).toBeTrue();
    superCalled = false;

    expect(new D()).toBe(bObject);

    expect(superCalled).toBeTrue();
});

test("cannot access super via indirect eval", () => {
    const indirect = eval;
    let superCalled = false;

    const aObject = { a: 1 };
    const bObject = { b: 1 };

    class A {
        constructor() {
            superCalled = true;
            this.a = aObject;
            this.c = bObject;
        }
    }

    class B extends A {
        constructor() {
            indirect("super()");
        }
    }

    expect(() => {
        new B();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(superCalled).toBeFalse();

    class C extends A {
        constructor() {
            super();
            return indirect("super.a");
        }
    }

    expect(() => {
        new C();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(superCalled).toBeTrue();
    superCalled = false;

    class D extends A {
        constructor() {
            super();
            return indirect("super['b']");
        }
    }

    expect(() => {
        new D();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(superCalled).toBeTrue();
});

test("super outside of derived class fails to parse", () => {
    expect("super").not.toEval();
    expect("super()").not.toEval();
    expect("super.a").not.toEval();
    expect("super['b']").not.toEval();
    expect("function a() { super }").not.toEval();
    expect("function a() { super() }").not.toEval();
    expect("function a() { super.a }").not.toEval();
    expect("function a() { super['b'] }").not.toEval();
    expect("() => { super }").not.toEval();
    expect("() => { super() }").not.toEval();
    expect("() => { super.a }").not.toEval();
    expect("() => { super['b'] }").not.toEval();
    expect("class A { constructor() { super } }").not.toEval();
    expect("class A { constructor() { super() } }").not.toEval();

    expect(() => {
        eval("super");
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        eval("super()");
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        eval("super.a");
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        eval("super['b']");
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    function a() {
        eval("super");
    }

    expect(() => {
        a();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        new a();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    function b() {
        eval("super()");
    }

    expect(() => {
        b();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        new b();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    function c() {
        eval("super.a");
    }

    expect(() => {
        c();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        new c();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    function d() {
        eval("super['b']");
    }

    expect(() => {
        d();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    expect(() => {
        new d();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    const e = () => eval("super");

    expect(() => {
        e();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    const f = () => eval("super()");

    expect(() => {
        f();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    const g = () => eval("super.a");

    expect(() => {
        g();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    const h = () => eval("super['b']");

    expect(() => {
        h();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    class I {
        constructor() {
            eval("super");
        }
    }

    expect(() => {
        new I();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");

    class J {
        constructor() {
            eval("super()");
        }
    }

    expect(() => {
        new J();
    }).toThrowWithMessage(SyntaxError, "'super' keyword unexpected here");
});
