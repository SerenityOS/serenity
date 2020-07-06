// This file must not be formatted by prettier. Make sure your IDE
// respects the .prettierignore file!

test("basic functionality", () => {
    const A = class {
        constructor(x) {
           this.x = x;
        }

        getX() {
           return this.x * 2;
        }
    };

    expect(new A(10).getX()).toBe(20);
});

test("inline instantiation", () => {
    const a = new class {
        constructor() {
            this.x = 10;
        }

        getX() {
            return this.x * 2;
        }
    };

    expect(a.getX()).toBe(20);
});

test("inline instantiation with argument", () => {
    const a = new class {
        constructor(x) {
            this.x = x;
        }

        getX() {
            return this.x * 2;
        }
    }(10);

    expect(a.getX()).toBe(20);
});

test("extending class expressions", () => {
    class A extends class {
        constructor(x) {
            this.x = x;
        }
    } {
        constructor(y) {
            super(y);
            this.y = y * 2;
        }
    };

    const a = new A(10);
    expect(a.x).toBe(10);
    expect(a.y).toBe(20);
});

test("class expression name", () => {
    let A = class {}
    expect(A.name).toBe("A");

    let B = class C {}
    expect(B.name).toBe("C");
});
