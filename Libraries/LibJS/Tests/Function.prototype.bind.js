load("test-common.js");

try {
    assert(Function.prototype.bind.length === 1);

    var charAt = String.prototype.charAt.bind("bar");
    assert(charAt(0) + charAt(1) + charAt(2) === "bar");

    function getB() {
        return this.toUpperCase().charAt(0);
    }
    assert(getB.bind("bar")() === "B");

    function sum(a, b, c) {
        return a + b + c;
    }

    // Arguments should be able to be bound to a function.
    var boundSum = sum.bind(null, 10, 5);
    assert(isNaN(boundSum()));

    assert(boundSum(5) === 20);
    assert(boundSum(5, 6, 7) === 20);

    // Arguments should be appended to a BoundFunction's bound arguments.
    assert(boundSum.bind(null, 5)() === 20);

    // A BoundFunction's length property should be adjusted based on the number
    // of bound arguments.
    assert(sum.length === 3);
    assert(boundSum.length === 1);
    assert(boundSum.bind(null, 5).length === 0);
    assert(boundSum.bind(null, 5, 6, 7, 8).length === 0);

    function identity() {
        return this;
    }

    // It should capture the global object if the |this| value is null or undefined.
    assert(identity.bind()() === globalThis);
    assert(identity.bind(null)() === globalThis);
    assert(identity.bind(undefined)() === globalThis);

    function Foo() {
        assert(identity.bind()() === globalThis);
        assert(identity.bind(this)() === this);
    }
    new Foo();

    // Primitive |this| values should be converted to objects.
    assert(identity.bind("foo")() instanceof String);
    assert(identity.bind(123)() instanceof Number);
    assert(identity.bind(true)() instanceof Boolean);

    // It should retain |this| values passed to it.
    var obj = { foo: "bar" };

    assert(identity.bind(obj)() === obj);

    // The bound |this| can not be changed once set
    assert(identity.bind("foo").bind(123)() instanceof String);

    // The bound |this| value should have no effect on a constructor.
    function Bar() {
        this.x = 3;
        this.y = 4;
    }
    Bar.prototype.baz = "baz";

    var BoundBar = Bar.bind({ u: 5, v: 6 });

    var bar = new BoundBar();
    assert(bar.x === 3);
    assert(bar.y === 4);
    assert(typeof bar.u === "undefined");
    assert(typeof bar.v === "undefined");
    // Objects constructed from BoundFunctions should retain the prototype of the original function.
    assert(bar.baz === "baz");
    // BoundFunctions should not have a prototype property.
    assert(typeof BoundBar.prototype === "undefined");

    // Function.prototype.bind should not accept non-function values.
    assertThrowsError(() => {
        Function.prototype.bind.call("foo");
    }, {
        error: TypeError,
        message: "Not a Function object"
    });

    // A constructor's arguments should be able to be bound.
    var Make5 = Number.bind(null, 5);
    assert(Make5() === 5);
    assert(new Make5().valueOf() === 5);

    // FIXME: Uncomment me when strict mode is implemented
    //     function strictIdentity() {
    //         return this;
    //     }

    //     assert(strictIdentity.bind()() === undefined);
    //     assert(strictIdentity.bind(null)() === null);
    //     assert(strictIdentity.bind(undefined)() === undefined);
    // })();

    // FIXME: Uncomment me when arrow functions have the correct |this| value.
    // // Arrow functions can not have their |this| value set.
    // assert((() => this).bind("foo")() === globalThis)

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
