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
