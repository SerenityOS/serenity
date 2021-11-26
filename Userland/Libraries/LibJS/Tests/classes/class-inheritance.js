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
