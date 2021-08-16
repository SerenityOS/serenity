/*
 * @test /nodynamiccopyright/
 * @bug     7123100
 * @summary javac fails with java.lang.StackOverflowError
 * @compile/fail/ref=T7123100d.out -Werror -Xlint:unchecked -XDrawDiagnostics T7123100d.java
 */

class T7123100d {
    <E extends Enum<E>> E m(Enum<E> e) {
        return null;
    }

    <Z> void test(Enum<?> e) {
        Z z = (Z)m(e);
    }
}
