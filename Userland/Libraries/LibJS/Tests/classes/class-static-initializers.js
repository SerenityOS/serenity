test("basic functionality", () => {
    var called = false;
    class A {
        static {
            expect(called).toBeFalse();
            expect(this.name).toBe("A");
            called = true;
        }
    }

    expect(called).toBeTrue();
    new A();
    expect(called).toBeTrue();
});

test("called in order", () => {
    var i = 0;
    class A {
        static {
            expect(i).toBe(0);
            i++;
        }

        static method() {
            return 2;
        }

        static {
            expect(i).toBe(1);
            i++;
        }
    }

    expect(i).toBe(2);
    new A();
    expect(i).toBe(2);
});

test("correct this", () => {
    var thisValue = null;
    class A {
        static {
            thisValue = this;
        }
    }

    expect(thisValue).toBe(A);
});
