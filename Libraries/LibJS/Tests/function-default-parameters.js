load("test-common.js");

try {
    function func1(a, b = 1) {
        return a + b;
    }

    const arrowFunc1 = (a, b = 1) => a + b;

    assert(func1(4, 5) === 9);
    assert(func1(4) === 5);
    assert(func1(4, undefined) === 5);
    assert(Number.isNaN(func1()));

    assert(arrowFunc1(4, 5) === 9);
    assert(arrowFunc1(4) === 5);
    assert(arrowFunc1(4, undefined) === 5);
    assert(Number.isNaN(arrowFunc1()));

    function func2(a = 6) {
        return typeof a;
    }

    const arrowFunc2 = (a = 6) => typeof a;

    assert(func2() === "number");
    assert(func2(5) === "number");
    assert(func2(undefined) === "number");
    assert(func2(false) === "boolean");
    assert(func2(null) === "object");
    assert(func2({}) === "object");

    assert(arrowFunc2() === "number");
    assert(arrowFunc2(5) === "number");
    assert(arrowFunc2(undefined) === "number");
    assert(arrowFunc2(false) === "boolean");
    assert(arrowFunc2(null) === "object");
    assert(arrowFunc2({}) === "object");

    function func3(a = 5, b) {
        return a + b;
    }

    const arrowFunc3 = (a = 5, b) => a + b;

    assert(func3(4, 5) === 9);
    assert(func3(undefined, 4) === 9);
    assert(Number.isNaN(func3()));

    assert(arrowFunc3(4, 5) === 9);
    assert(arrowFunc3(undefined, 4) === 9);
    assert(Number.isNaN(arrowFunc3()));

    function func4(a, b = a) {
        return a + b;
    }

    const arrowFunc4 = (a, b = a) => a + b;

    assert(func4(4, 5) === 9);
    assert(func4(4) === 8);
    assert(func4("hf") === "hfhf");
    assert(func4(true) === 2);
    assert(Number.isNaN(func4()));

    assert(arrowFunc4(4, 5) === 9);
    assert(arrowFunc4(4) === 8);
    assert(arrowFunc4("hf") === "hfhf");
    assert(arrowFunc4(true) === 2);
    assert(Number.isNaN(arrowFunc4()));

    function func5(a = function() { return 5; }) { 
      return a();
    }

    const arrowFunc5 = (a = function() { return 5; }) => a();

    assert(func5() === 5);
    assert(func5(function() { return 10; }) === 10);
    assert(func5(() => 10) === 10);
    assert(arrowFunc5() === 5);
    assert(arrowFunc5(function() { return 10; }) === 10);
    assert(arrowFunc5(() => 10) === 10);

    function func6(a = () => 5) {
      return a();
    }

    const arrowFunc6 = (a = () => 5) => a();
    
    assert(func6() === 5);
    assert(func6(function() { return 10; }) === 10);
    assert(func6(() => 10) === 10);
    assert(arrowFunc6() === 5);
    assert(arrowFunc6(function() { return 10; }) === 10);
    assert(arrowFunc6(() => 10) === 10);

    function func7(a = { foo: "bar" }) {
      return a.foo;
    }

    const arrowFunc7 = (a = { foo: "bar" }) => a.foo

    assert(func7() === "bar");
    assert(func7({ foo: "baz" }) === "baz");
    assert(arrowFunc7() === "bar");
    assert(arrowFunc7({ foo: "baz" }) === "baz");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
