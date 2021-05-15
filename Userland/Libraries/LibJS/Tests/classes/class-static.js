test("basic functionality", () => {
    class A {
        static method() {
            return 10;
        }
    }

    expect(A.method()).toBe(10);
    expect(new A().method).toBeUndefined();
});

test("extended name syntax", () => {
    class A {
        static method() {
            return 1;
        }

        static 12() {
            return 2;
        }

        static [`he${"llo"}`]() {
            return 3;
        }
    }

    expect(A.method()).toBe(1);
    expect(A[12]()).toBe(2);
    expect(A.hello()).toBe(3);
});

test("bound |this|", () => {
    class A {
        static method() {
            expect(this).toBe(A);
        }
    }

    A.method();
});

test("inherited static methods", () => {
    class Parent {
        static method() {
            return 3;
        }
    }

    class Child extends Parent {}

    expect(Parent.method()).toBe(3);
    expect(Child.method()).toBe(3);
    expect(new Parent()).not.toHaveProperty("method");
    expect(new Child()).not.toHaveProperty("method");
});

test("static method overriding", () => {
    class Parent {
        static method() {
            return 3;
        }
    }

    class Child extends Parent {
        static method() {
            return 10;
        }
    }

    expect(Parent.method()).toBe(3);
    expect(Child.method()).toBe(10);
});
