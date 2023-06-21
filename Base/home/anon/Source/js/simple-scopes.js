 //I should return `undefined` because y is bound to the inner-most enclosing function, i.e the nested one (bar()), therefore, it's undefined in the scope of foo()
function foo() {
    function bar() {
        var y = 6;
    }

    bar();
    return y;
}

console.log(foo());
