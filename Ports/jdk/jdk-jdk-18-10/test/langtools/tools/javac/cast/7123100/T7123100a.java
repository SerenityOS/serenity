/*
 * @test /nodynamiccopyright/
 * @bug     7123100
 * @summary javac fails with java.lang.StackOverflowError
 * @compile/fail/ref=T7123100a.out -Werror -Xlint:unchecked -XDrawDiagnostics T7123100a.java
 */

class T7123100a {
    <E extends Enum<E>> E m() {
        return null;
    }

    <Z> void test() {
        Z z = (Z)m();
    }
}
