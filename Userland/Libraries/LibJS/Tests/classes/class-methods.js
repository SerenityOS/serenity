test("basic functionality", () => {
    class A {
        number() {
            return 2;
        }

        string() {
            return "foo";
        }
    }

    const a = new A();
    expect(a.number()).toBe(2);
    expect(a.string()).toBe("foo");
});

test("length", () => {
    class A {
        method1() {}

        method2(a, b, c, d) {}

        method3(a, b, ...c) {}
    }

    const a = new A();
    expect(a.method1).toHaveLength(0);
    expect(a.method2).toHaveLength(4);
    expect(a.method3).toHaveLength(2);
});

test("extended name syntax", () => {
    class A {
        "method with space"() {
            return 1;
        }

        12() {
            return 2;
        }

        [`he${"llo"}`]() {
            return 3;
        }
    }

    const a = new A();
    expect(a["method with space"]()).toBe(1);
    expect(a[12]()).toBe(2);
    expect(a.hello()).toBe(3);
});
