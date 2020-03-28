function Foo() {
    this.x = 123;
}

var foo = new Foo();
if (foo.x === 123)
    console.log("PASS");
