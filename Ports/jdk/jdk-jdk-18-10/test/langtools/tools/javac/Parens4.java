/*
 * @test /nodynamiccopyright/
 * @bug 4933317
 * @summary javac accepts parens in package names
 * @author gafter
 *
 * @compile/fail/ref=Parens4.out -XDrawDiagnostics Parens4.java
 */

class Parens4 {
    Object f() {
        return (java).util.Collections.EMPTY_LIST;
    }
}
