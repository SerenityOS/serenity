load("test-common.js");

try {
  class X {
    constructor() {
      this.x = 3;
    }
    getX() {
      return 3;
    }

    init() {
      this.y = 3;
    }
  }

  assert(X.name === "X");
  assert(X.length === 0);

  class Y extends X {
    init() {
      super.init();
      this.y += 3;
    }
  }

  assert(new Y().getX() === 3);
  assert(new Y().x === 3);

  let x = new X();
  assert(x.x === 3);
  assert(x.getX() === 3);

  let y = new Y();
  assert(y.x === 3);
  assert(y.y === undefined);
  y.init();
  assert(y.y === 6);
  assert(y.hasOwnProperty("y"));

  class Foo {
    constructor(x) {
      this.x = x;
    }
  }
  assert(Foo.length === 1);

  class Bar extends Foo {
    constructor() {
      super(5);
    }
  }

  class Baz {
    constructor() {
      this.foo = 55;
      this._bar = 33;
    }

    get bar() {
      return this._bar;
    }

    set bar(value) {
      this._bar = value;
    }

    ["get" + "Foo"]() {
      return this.foo;
    }

    static get staticFoo() {
      assert(this === Baz);
      return 11;
    }
  }

  let barPropertyDescriptor = Object.getOwnPropertyDescriptor(Baz.prototype, "bar");
  assert(barPropertyDescriptor.get.name === "get bar");
  assert(barPropertyDescriptor.set.name === "set bar");

  let baz = new Baz();
  assert(baz.foo === 55);
  assert(baz.bar === 33);
  baz.bar = 22;
  assert(baz.bar === 22);

  assert(baz.getFoo() === 55);
  assert(Baz.staticFoo === 11);

  assert(new Bar().x === 5);

  class ExtendsFunction extends function () {
    this.foo = 22;
  } {}
  assert(new ExtendsFunction().foo === 22);

  class ExtendsString extends String {}
  assert(new ExtendsString() instanceof String);
  assert(new ExtendsString() instanceof ExtendsString);
  assert(new ExtendsString("abc").charAt(1) === "b");

  class MyWeirdString extends ExtendsString {
    charAt(i) {
      return "#" + super.charAt(i);
    }
  }
  assert(new MyWeirdString("abc").charAt(1) === "#b");

  class ExtendsNull extends null {}

  assertThrowsError(
    () => {
      new ExtendsNull();
    },
    {
      error: ReferenceError,
    }
  );
  assert(Object.getPrototypeOf(ExtendsNull.prototype) === null);

  class ExtendsClassExpression extends class {
    constructor(x) {
      this.x = x;
    }
  } {
    constructor(y) {
      super(5);
      this.y = 6;
    }
  }
  let extendsClassExpression = new ExtendsClassExpression();
  assert(extendsClassExpression.x === 5);
  assert(extendsClassExpression.y === 6);

  class InStrictMode {
    constructor() {
      assert(isStrictMode());
    }

    method() {
      assert(isStrictMode());
    }
  }

  let resultOfAnExpression = new (class {
    constructor(x) {
      this.x = x;
    }
    getX() {
      return this.x + 10;
    }
  })(55);
  assert(resultOfAnExpression.x === 55);
  assert(resultOfAnExpression.getX() === 65);

  let ClassExpression = class Foo {};
  assert(ClassExpression.name === "Foo");

  new InStrictMode().method();
  assert(!isStrictMode());

  assertIsSyntaxError(`
        class GetterWithArgument {
            get foo(bar) {
                return 0;
            }
        }
    `);

  assertIsSyntaxError(`
        class SetterWithNoArgumetns {
            set foo() {
            }
        }
    `);

  assertIsSyntaxError(`
        class SetterWithMoreThanOneArgument {
            set foo(bar, baz) {
            }
        }
    `);

  assertIsSyntaxError(`
        class FooBase {}
        class IsASyntaxError extends FooBase {
            bar() {
                function f() { super.baz; }
            }
        }
    `);

  assertIsSyntaxError(`
        class NoBaseSuper {
            constructor() {
                super();
            }
        }
    `);

  assertThrowsError(
    () => {
      class BadExtends extends 3 {}
    },
    {
      error: TypeError,
    }
  );

  assertThrowsError(
    () => {
      class BadExtends extends undefined {}
    },
    {
      error: TypeError,
    }
  );

  class SuperNotASyntaxError {
    bar() {
      () => {
        super.baz;
      };
    }
  }

  class SuperNoBasePropertyLookup {
    constructor() {
      super.foo;
    }
  }

  assertThrowsError(
    () => {
      class Base {}
      class DerivedDoesntCallSuper extends Base {
        constructor() {
          this;
        }
      }

      new DerivedDoesntCallSuper();
    },
    {
      error: ReferenceError,
    }
  );
  assertThrowsError(
    () => {
      class Base {}
      class CallsSuperTwice extends Base {
        constructor() {
          super();
          super();
        }
      }

      new CallsSuperTwice();
    },
    {
      error: ReferenceError,
    }
  );

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
