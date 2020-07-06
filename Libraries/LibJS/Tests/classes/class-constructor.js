test("property initialization", () => {
    class A {
        constructor() {
            this.x = 3;
        }
    }

    expect(new A().x).toBe(3);
});

test("method initialization", () => {
    class A {
        constructor() {
            this.x = () => 10;
        }
    }

    expect(new A().x()).toBe(10);
});

test("initialize to class method", () => {
    class A {
        constructor() {
            this.x = this.method;
        }

        method() {
            return 10;
        }
    }

    expect(new A().x()).toBe(10);
});

test("constructor length affects class length", () => {
    class A {
        constructor() {}
    }

    expect(A).toHaveLength(0);

    class B {
        constructor(a, b, c = 2) {}
    }

    expect(B).toHaveLength(2);
});

test.skip("must be invoked with 'new'", () => {
    class A {
        constructor() {}
    }

    expect(() => {
        A();
    }).toThrow(TypeError); // FIXME: Add message when this test works

    expect(() => {
        A.prototype.constructor();
    }).toThrow(TypeError); // FIXME: Add message when this test works
});

test("implicit constructor", () => {
    class A {}

    expect(new A()).toBeInstanceOf(A);
});
