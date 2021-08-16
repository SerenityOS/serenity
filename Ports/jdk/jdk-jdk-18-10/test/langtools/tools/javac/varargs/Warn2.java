/*
 * @test /nodynamiccopyright/
 * @bug 5024518
 * @summary need warning if varargs argument isn't boxed
 * @author gafter
 *
 * @compile                           Warn2.java
 * @compile/fail/ref=Warn2.out -XDrawDiagnostics   -Werror             Warn2.java
 * @compile       -Werror -Xlint:none Warn2.java
 */

package varargs.warn2;

class T {
    static void f(String fmt, Object... args) {}

    public static void main(String[] args) {
        f("foo", null);
    }
}
