function foo() {
    var a = [];
    for (var i = 0; i < 4000; ++i) {
        a.push("string" + i);
    }
}

foo();
