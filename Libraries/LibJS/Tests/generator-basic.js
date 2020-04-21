load("test-common.js")

function* generator0(i) {
    yield i;
    yield i + 10;
}

function* idMaker(index) {
    for(;;)
        yield index++;
}

function* logGenerator() {
    var a = [];
    a.push(yield a);
    a.push(yield a);
    a.push(yield a);
    a.push(yield a);
    a.push(yield a);
    return a;
}

function* yieldAndReturn() {
  yield "Y";
  return "R";
  yield "unreachable";
}

try {
    let gen = generator0(10)
    assert(gen.next().value == 10)
    assert(gen.next().value == 20)
    assert(gen.next().done == true)

    gen = idMaker(0);
    assert(gen.next().value == 0)
    assert(gen.next().value == 1)
    assert(gen.next().value == 2)
    assert(gen.next().value == 3)

    gen = logGenerator();
    assert(gen.next('you shall not see me').value.length == 0);
    assert(gen.next('foo').value.length == 1);
    assert(gen.next('bar').value.length == 2);
    assert(gen.next('baz').value.length == 3);
    assert(gen.next('fub').value.length == 4);
    let log = gen.next().value;
    assert(log[0] != 'you shall not see me');
    assert(log[3] == 'fub');

    gen = yieldAndReturn()
    assert(gen.next().value == 'Y');
    assert(gen.next().value == 'R');
    assert(gen.next().value == undefined);

    console.log("PASS")
} catch(e) {
    console.log("FAIL: " + e)
}
