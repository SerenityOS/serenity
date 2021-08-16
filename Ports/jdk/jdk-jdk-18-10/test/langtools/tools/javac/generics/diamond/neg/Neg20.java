/*
 * @test /nodynamiccopyright/
 * @bug 8078592
 * @summary Compiler fails to reject erroneous use of diamond with anonymous classes involving "fresh" type variables.
 * @compile/fail/ref=Neg20.out Neg20.java -XDrawDiagnostics
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public class Neg20 {
    static class Foo<E extends B<E>> {
        public Foo<E> complexMethod(E a) {
            return this;
        }
    }

    static class Goo<@T E> {
        public Goo<E> complexMethod(E a) {
            return this;
        }
    }

    static class B<V> {
    }

    @Target(ElementType.TYPE_USE)
    static @interface T {
    }

    public static void check() {
        Foo<?> t4 = new Foo<>() {
        };
        Goo<?> g4 = new Goo<>() {
        };
    }
}
