load("test-common.js");

try {
    var o = {};
    Object.defineProperty(o, "foo", { value: 1, writable: false, enumerable: false });

    assert(o.foo === 1);
    o.foo = 2;
    assert(o.foo === 1);

    var d = Object.getOwnPropertyDescriptor(o, "foo");
    assert(d.configurable === false);
    assert(d.enumerable === false);
    assert(d.writable === false);
    assert(d.value === 1);

    Object.defineProperty(o, "bar", { value: "hi", writable: true, enumerable: true });

    assert(o.bar === "hi");
    o.bar = "ho";
    assert(o.bar === "ho");

    d = Object.getOwnPropertyDescriptor(o, "bar");
    assert(d.configurable === false);
    assert(d.enumerable === true);
    assert(d.writable === true);
    assert(d.value === "ho");

    assertThrowsError(() => {
        Object.defineProperty(o, "bar", { value: "xx", enumerable: false });
    }, {
        error: TypeError
    });

    Object.defineProperty(o, "baz", { value: 9, configurable: true, writable: false });
    Object.defineProperty(o, "baz", { configurable: true, writable: true });

    d = Object.getOwnPropertyDescriptor(o, "baz");
    assert(d.configurable === true);
    assert(d.writable === true);
    assert(d.value === 9);

    Object.defineProperty(o, "qux", {
        configurable: true,
        get() {
            return o.secret_qux + 1;
        },
        set(value) {
            this.secret_qux = value + 1;
        },
    });

    o.qux = 10;
    assert(o.qux === 12);
    o.qux = 20;
    assert(o.qux = 22);

    Object.defineProperty(o, "qux", { configurable: true, value: 4 });

    assert(o.qux === 4);
    o.qux = 5;
    assert(o.qux = 4);

    Object.defineProperty(o, "qux", {
        configurable: false,
        get() {
            return this.secret_qux + 2;
        },
        set(value) {
            o.secret_qux = value + 2;
        },
    });

    o.qux = 10;
    assert(o.qux === 14);
    o.qux = 20;
    assert(o.qux = 24);

    assertThrowsError(() => {
        Object.defineProperty(o, "qux", {
            configurable: false,
            get() {
                return this.secret_qux + 2;
            },
        });
    }, {
        error: TypeError,
        message: "Cannot change attributes of non-configurable property 'qux'",
    });

    assertThrowsError(() => {
        Object.defineProperty(o, "qux", { value: 2 });
    }, {
        error: TypeError,
        message: "Cannot change attributes of non-configurable property 'qux'",
    });

    assertThrowsError(() => {
        Object.defineProperty(o, "a", {
            get() {},
            value: 9,
        });
    }, {
        error: TypeError,
        message: "Accessor property descriptors cannot specify a value or writable key",
    });

    assertThrowsError(() => {
        Object.defineProperty(o, "a", {
            set() {},
            writable: true,
        });
    }, {
        error: TypeError,
        message: "Accessor property descriptors cannot specify a value or writable key",
    });

    console.log("PASS");
} catch (e) {
    console.log(e)
}
