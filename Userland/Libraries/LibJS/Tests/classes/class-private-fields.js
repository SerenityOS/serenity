test("basic functionality", () => {
    class A {
        #number = 3;

        getNumber() {
            return this.#number;
        }

        #string = "foo";

        getString() {
            return this.#string;
        }

        #uninitialized;

        getUninitialized() {
            return this.#uninitialized;
        }
    }

    const a = new A();
    expect(a.getNumber()).toBe(3);
    expect(a.getString()).toBe("foo");
    expect(a.getUninitialized()).toBeUndefined();

    expect("a.#number").not.toEval();
    expect("a.#string").not.toEval();
    expect("a.#uninitialized").not.toEval();
});

test("initializer has correct this value", () => {
    class A {
        #thisVal = this;

        getThisVal() {
            return this.#thisVal;
        }

        #thisName = this.#thisVal;

        getThisName() {
            return this.#thisName;
        }
    }

    const a = new A();
    expect(a.getThisVal()).toBe(a);
    expect(a.getThisName()).toBe(a);
});

test("static fields", () => {
    class A {
        static #simple = 1;

        static getStaticSimple() {
            return this.#simple;
        }

        static #thisVal = this;
        static #thisName = this.name;
        static #thisVal2 = this.#thisVal;

        static getThisVal() {
            return this.#thisVal;
        }

        static getThisName() {
            return this.#thisName;
        }

        static getThisVal2() {
            return this.#thisVal2;
        }
    }

    expect(A.getStaticSimple()).toBe(1);

    expect(A.getThisVal()).toBe(A);
    expect(A.getThisName()).toBe("A");
    expect(A.getThisVal2()).toBe(A);

    expect("A.#simple").not.toEval();
});

test("slash after private identifier is treated as division", () => {
    class A {
        static #field = 4;
        static #divided = this.#field / 2;

        static getDivided() {
            return this.#divided;
        }
    }

    expect(A.getDivided()).toBe(2);
});

test("private identifier not followed by 'in' throws", () => {
    expect(`class A { #field = 2; method() { return #field instanceof 1; }}`).not.toEval();
    expect(`class A { #field = 2; method() { return #field < 1; }}`).not.toEval();
    expect(`class A { #field = 2; method() { return #field + 1; }}`).not.toEval();
    expect(`class A { #field = 2; method() { return #field ** 1; }}`).not.toEval();
    expect(`class A { #field = 2; method() { return !#field; } }`).not.toEval();
    expect(`class A { #field = 2; method() { return ~#field; } }`).not.toEval();
    expect(`class A { #field = 2; method() { return ++#field; } }`).not.toEval();

    expect(`class A { #field = 2; method() { return #field in 1; }}`).toEval();
});

test("cannot have static and non static field with the same description", () => {
    expect("class A { static #simple; #simple; }").not.toEval();
});
