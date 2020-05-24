load("test-common.js");

try {
    let getNumber = () => 42;
    assert(getNumber() === 42);

    getNumber = () => 99;
    assert(getNumber() === 99);

    let add = (a, b) => a + b;
    assert(add(2, 3) === 5);

    const addBlock = (a, b) => {
        let res = a + b;
        return res;
    };
    assert(addBlock(5, 4) === 9);

    const makeObject = (a, b) => ({ a, b });
    const obj = makeObject(33, 44);
    assert(typeof obj === "object");
    assert(obj.a === 33);
    assert(obj.b === 44);

    let returnUndefined = () => { };
    assert(typeof returnUndefined() === "undefined");

    const makeArray = (a, b) => [a, b];
    const array = makeArray("3", { foo: 4 });
    assert(array[0] === "3");
    assert(array[1].foo === 4);

    let square = x => x * x;
    assert(square(3) === 9);

    let squareBlock = x => {
        return x * x;
    };
    assert(squareBlock(4) === 16);

    const message = (who => "Hello " + who)("friends!");
    assert(message === "Hello friends!");

    const sum = ((x, y, z) => x + y + z)(1, 2, 3);
    assert(sum === 6);

    const product = ((x, y, z) => {
        let res = x * y * z;
        return res;
    })(5, 4, 2);
    assert(product === 40);

    const half = (x => {
        return x / 2;
    })(10);
    assert(half === 5);

    var foo, bar;
    foo = bar, baz => {};
    assert(foo === undefined);
    assert(bar === undefined);

    console.log("PASS");
} catch {
    console.log("FAIL");
}
