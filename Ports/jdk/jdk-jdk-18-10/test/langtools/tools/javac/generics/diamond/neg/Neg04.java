/*
 * @test /nodynamiccopyright/
 * @bug 6939620 7020044 8062373
 *
 * @summary  Check that diamond fails when inference violates declared bounds
 *           (test with local class, qualified/simple type expressions)
 * @author mcimadamore
 * @compile/fail/ref=Neg04.out Neg04.java -XDrawDiagnostics
 *
 */

class Neg04 {

    void test() {
        class Foo<V extends Number> {
            Foo(V x) {}
            <Z> Foo(V x, Z z) {}
        }
        Foo<String> n1 = new Foo<>("");
        Foo<? extends String> n2 = new Foo<>("");
        Foo<?> n3 = new Foo<>("");
        Foo<? super String> n4 = new Foo<>("");

        Foo<String> n5 = new Foo<>("", "");
        Foo<? extends String> n6 = new Foo<>("", "");
        Foo<?> n7 = new Foo<>("", "");
        Foo<? super String> n8 = new Foo<>("", "");

        Foo<String> n9 = new Foo<>(""){};
        Foo<? extends String> n10 = new Foo<>(""){};
        Foo<?> n11 = new Foo<>(""){};
        Foo<? super String> n12 = new Foo<>(""){};

        Foo<String> n13 = new Foo<>("", ""){};
        Foo<? extends String> n14 = new Foo<>("", ""){};
        Foo<?> n15 = new Foo<>("", ""){};
        Foo<? super String> n16 = new Foo<>("", ""){};
    }
}
