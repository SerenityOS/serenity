/*
 * @test /nodynamiccopyright/
 * @bug 6939620 7020044 8062373
 *
 * @summary  Check that diamond fails when inference violates declared bounds
 *           (test with inner class, qualified/simple type expressions)
 * @author mcimadamore
 * @compile/fail/ref=Neg03.out Neg03.java -XDrawDiagnostics
 *
 */

class Neg03<U> {

    class Foo<V extends Number> {
        Foo(V x) {}
        <Z> Foo(V x, Z z) {}
    }

    void testSimple() {
        Foo<String> f1 = new Foo<>("");
        Foo<? extends String> f2 = new Foo<>("");
        Foo<?> f3 = new Foo<>("");
        Foo<? super String> f4 = new Foo<>("");

        Foo<String> f5 = new Foo<>("", "");
        Foo<? extends String> f6 = new Foo<>("", "");
        Foo<?> f7 = new Foo<>("", "");
        Foo<? super String> f8 = new Foo<>("", "");

        Foo<String> f9 = new Foo<>(""){};
        Foo<? extends String> f10 = new Foo<>(""){};
        Foo<?> f11 = new Foo<>(""){};
        Foo<? super String> f12 = new Foo<>(""){};

        Foo<String> f13 = new Foo<>("", ""){};
        Foo<? extends String> f14 = new Foo<>("", ""){};
        Foo<?> f15 = new Foo<>("", ""){};
        Foo<? super String> f16 = new Foo<>("", ""){};
    }

    void testQualified_1() {
        Foo<String> f1 = new Neg03<U>.Foo<>("");
        Foo<? extends String> f2 = new Neg03<U>.Foo<>("");
        Foo<?> f3 = new Neg03<U>.Foo<>("");
        Foo<? super String> f4 = new Neg03<U>.Foo<>("");

        Foo<String> f5 = new Neg03<U>.Foo<>("", "");
        Foo<? extends String> f6 = new Neg03<U>.Foo<>("", "");
        Foo<?> f7 = new Neg03<U>.Foo<>("", "");
        Foo<? super String> f8 = new Neg03<U>.Foo<>("", "");

        Foo<String> f9 = new Neg03<U>.Foo<>(""){};
        Foo<? extends String> f10 = new Neg03<U>.Foo<>(""){};
        Foo<?> f11 = new Neg03<U>.Foo<>(""){};
        Foo<? super String> f12 = new Neg03<U>.Foo<>(""){};

        Foo<String> f13 = new Neg03<U>.Foo<>("", ""){};
        Foo<? extends String> f14 = new Neg03<U>.Foo<>("", ""){};
        Foo<?> f15 = new Neg03<U>.Foo<>("", ""){};
        Foo<? super String> f16 = new Neg03<U>.Foo<>("", ""){};
    }

    void testQualified_2(Neg03<U> n) {
        Foo<String> f1 = n.new Foo<>("");
        Foo<? extends String> f2 = n.new Foo<>("");
        Foo<?> f3 = n.new Foo<>("");
        Foo<? super String> f4 = n.new Foo<>("");

        Foo<String> f5 = n.new Foo<>("", "");
        Foo<? extends String> f6 = n.new Foo<>("", "");
        Foo<?> f7 = n.new Foo<>("", "");
        Foo<? super String> f8 = n.new Foo<>("", "");

        Foo<String> f9 = n.new Foo<>(""){};
        Foo<? extends String> f10 = n.new Foo<>(""){};
        Foo<?> f11 = n.new Foo<>(""){};
        Foo<? super String> f12 = n.new Foo<>(""){};

        Foo<String> f13 = n.new Foo<>("", ""){};
        Foo<? extends String> f14 = n.new Foo<>("", ""){};
        Foo<?> f15 = n.new Foo<>("", ""){};
        Foo<? super String> f16 = n.new Foo<>("", ""){};
    }
}
