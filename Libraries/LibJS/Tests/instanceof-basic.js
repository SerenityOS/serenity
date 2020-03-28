function assert(x) { if (!x) throw 1; }

try {
    function Foo() {
        this.x = 123;
    }

    var foo = new Foo();
    assert(foo instanceof Foo);

    function Base() {
        this.is_base = true;
    }

    function Derived() {
        this.is_derived = true;
    }

    Object.setPrototypeOf(Derived.prototype, Base.prototype);

    var d = new Derived();
    assert(d instanceof Derived);
    assert(d instanceof Base);

    console.log("PASS");
} catch(e) {
    console.log("FAIL: " + e);
}
