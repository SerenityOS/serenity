/*
 * @test /nodynamiccopyright/
 * @bug 5024518
 * @summary need warning if varargs argument isn't boxed
 * @author gafter
 *
 * @compile Warn1.java
 * @compile/ref=Warn1.out -XDrawDiagnostics Warn1.java
 * @compile -Werror -Xlint:none Warn1.java
 */

package varargs.warn1;

class T {
    static void f(String fmt, Object... args) {}

    public static void main(String[] args) {
        f("foo", args);
    }
}
