/*
 * @test /nodynamiccopyright/
 * @bug     7123100
 * @summary javac fails with java.lang.StackOverflowError
 * @compile/fail/ref=T7123100b.out -Werror -Xlint:unchecked -XDrawDiagnostics T7123100b.java
 */

class T7123100b {
    <Z> void test(Enum<?> e) {
        Z z = (Z)e;
    }
}
