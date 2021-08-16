/*
 * @test /nodynamiccopyright/
 * @bug     7123100
 * @summary javac fails with java.lang.StackOverflowError
 * @compile/fail/ref=T7123100c.out -Werror -Xlint:unchecked -XDrawDiagnostics T7123100c.java
 */

class T7123100c {
    <E> E m(E e) {
        return null;
    }

    <Z> void test(Enum<?> e) {
        Z z = (Z)m(e);
    }
}
