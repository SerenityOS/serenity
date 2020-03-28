function Foo() {
    this.x = 123;
}

var foo = new Foo();
if (foo instanceof Foo)
    console.log("PASS");
